// Global variables to manage state
window.currentExpenseId = null;

// --- PAGINATION STATE ---
const PAGE_SIZE = 20;
let currentPage = 0;
let hasMoreExpenses = true;

// Helper function to handle rounding to two decimal places for currency
const roundToTwoDecimals = (num) => Math.round(num * 100) / 100;

// --- DOM Element Variables ---
const expenseForm = document.getElementById('add-expense-form');
const amountInput = document.getElementById('amount');
const descriptionInput = document.getElementById('description');
const paidByInput = document.getElementById('paidBy');
const splitTableBody = document.getElementById('expense-split-table-body');
const submitExpenseBtn = document.getElementById('submit-expense-btn');
const expensesList = document.getElementById('expenses-list');
const noExpensesEl = document.getElementById('no-expenses');
const loadMoreBtn = document.getElementById('load-more-expenses-btn');
const modalTitle = document.getElementById('expense-modal-title');
const owedTotalSummaryEl = document.getElementById('owed-total-summary');
const remainingAmountEl = document.getElementById('remaining-amount');

// ----------------------------------------------------------------------------------
// --- AUTHENTICATION & GROUP MEMBERSHIP HELPERS ---
// ----------------------------------------------------------------------------------

function decodeJwtToken(token) {
    if (!token) return null;
    try {
        const payload = token.split('.')[1];
        const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
        return JSON.parse(decoded);
    } catch {
        return null;
    }
}

function isTokenExpired(token) {
    const payload = decodeJwtToken(token);
    if (!payload || !payload.exp) return true;
    
    const expiry = payload.exp * 1000; // Convert to milliseconds
    const now = Date.now();
    return now >= expiry;
}

function handleAuthError(message = "Authentication failed. Please log in again.") {
    console.error('Auth error:', message);
    localStorage.removeItem('jwtToken');
    window.alertUserMessage(message, 'error');
    setTimeout(() => {
        window.location.href = 'login.html';
    }, 2000);
}

function isUserMemberOfGroup(jwtUsername) {
    if (!window.currentGroupMembers || !Array.isArray(window.currentGroupMembers)) {
        console.error('No group members data available');
        return false;
    }

    const isMember = window.currentGroupMembers.some(member => {
        const memberUsername = typeof member === 'object' ? member.username : member;
        return memberUsername?.toLowerCase() === jwtUsername.toLowerCase();
    });

    return isMember;
}

async function validateTokenAndGroupMembership(expenseId = null) {
    let token = localStorage.getItem('jwtToken');
    
    if (!token) {
        handleAuthError("No authentication token found.");
        return null;
    }

    if (isTokenExpired(token)) {
        handleAuthError("Your session has expired.");
        return null;
    }

    const payload = decodeJwtToken(token);
    if (!payload) {
        handleAuthError("Invalid authentication token.");
        return null;
    }

    const jwtUsername = payload.username || payload.sub;

    if (!isUserMemberOfGroup(jwtUsername)) {
        window.alertUserMessage("You are not a member of this group. Please join the group first.", 'error');
        return null;
    }

    if (expenseId) {
        // For expense operations, we'll check permissions with fresh data
        const expense = await window.fetchExpenseById(window.currentGroupId, expenseId);
        if (expense) {
            const isPayer = expense.paidBy?.toLowerCase() === jwtUsername.toLowerCase();
            if (!isPayer) {
                window.alertUserMessage("You can only update expenses that you paid for.", 'error');
                return null;
            }
        } else {
            console.error('Expense not found for ID:', expenseId);
            window.alertUserMessage("Expense not found.", 'error');
            return null;
        }
    }

    return { token, jwtUsername };
}

// ----------------------------------------------------------------------------------
// --- DATA FETCHING ---
// ----------------------------------------------------------------------------------

window.fetchExpensesPage = async function(groupId, page, initialLoad = false) {
    if (!groupId || (!hasMoreExpenses && !initialLoad)) return;

    let token = localStorage.getItem('jwtToken');
    if (!token) {
        window.handleError("Authentication failed. Please log in.");
        return;
    }

    if (initialLoad) {
        expensesList.innerHTML = '<div class="text-center py-8 text-gray-500 font-semibold">Loading expenses...</div>';
        loadMoreBtn?.classList.add('hidden');
    } else if (loadMoreBtn) {
        loadMoreBtn.textContent = 'Loading...';
        loadMoreBtn.disabled = true;
    }

    try {
        const expenseUrl = `${window.apiUrlBase}${groupId}/expenses?page=${page}&size=${PAGE_SIZE}`;
        console.log('Fetching expenses from:', expenseUrl);
        const expenseResponse = await fetch(expenseUrl, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!expenseResponse.ok) {
            if (expenseResponse.status === 403) {
                const errorText = await expenseResponse.text();
                let errorMessage = "Access denied to fetch expenses.";
                try {
                    const errorJson = JSON.parse(errorText);
                    errorMessage = errorJson.message || errorJson.error || errorMessage;
                } catch (e) {
                    // Use raw error text
                }
                window.handleError(errorMessage);
                return;
            }
            throw new Error(`Failed to fetch expenses: ${expenseResponse.status} ${await expenseResponse.text()}`);
        }

        const newExpenses = await expenseResponse.json();
        console.log('Fetched expenses:', newExpenses);
        
        hasMoreExpenses = newExpenses.length === PAGE_SIZE;

        window.renderExpenses(newExpenses, initialLoad);

        if (!initialLoad) currentPage++;

    } catch (error) {
        window.handleError(`Error loading expenses: ${error.message}`);
        if (initialLoad) {
             expensesList.innerHTML = '<div class="text-center py-8 text-red-500 font-semibold">Failed to load data.</div>';
        }
    } finally {
        if (loadMoreBtn) {
            loadMoreBtn.textContent = 'Load More';
            loadMoreBtn.disabled = !hasMoreExpenses;
            if (!hasMoreExpenses) {
                loadMoreBtn.classList.add('hidden');
            } else {
                loadMoreBtn.classList.remove('hidden');
            }
        }
    }
};

// Fetch single expense from backend - ALWAYS FETCH FRESH DATA - NO CACHED FALLBACK
window.fetchExpenseById = async function(groupId, expenseId) {
    let token = localStorage.getItem('jwtToken');
    if (!token) {
        window.handleError("Authentication failed. Please log in.");
        return null;
    }

    try {
        const expenseUrl = `${window.apiUrlBase}${groupId}/expenses/${expenseId}`;
        console.log('🔍 Fetching FRESH expense data from:', expenseUrl);
        const expenseResponse = await fetch(expenseUrl, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!expenseResponse.ok) {
            // ❌ REMOVED ALL CACHED DATA FALLBACKS - ALWAYS THROW ERROR
            if (expenseResponse.status === 404) {
                throw new Error(`Expense not found: ${expenseId}`);
            }
            throw new Error(`Failed to fetch expense: ${expenseResponse.status} ${await expenseResponse.text()}`);
        }

        const expense = await expenseResponse.json();
        console.log('✅ Successfully fetched FRESH expense data:', expense);
        return expense;

    } catch (error) {
        console.error('❌ Error fetching single expense:', error);
        // ❌ REMOVED CACHED DATA FALLBACK - ALWAYS THROW ERROR
        throw error;
    }
};

// ----------------------------------------------------------------------------------
// --- CORE EXPENSE DISPLAY AND EDIT/VIEW SETUP ---
// ----------------------------------------------------------------------------------

window.renderExpenses = function(newExpenses, initialLoad = false) {
    if (!expensesList || !noExpensesEl) return;

    if (initialLoad) {
        expensesList.innerHTML = '';
        window.currentExpenses = [];
    }

    const sortedNewExpenses = newExpenses.sort((a, b) => (new Date(b.date || 0).getTime()) - (new Date(a.date || 0).getTime()));
    window.currentExpenses.push(...sortedNewExpenses);

    window.currentExpenses = Array.from(new Set(window.currentExpenses.map(exp => exp.id)))
        .map(id => window.currentExpenses.find(exp => exp.id === id))
        .sort((a, b) => (new Date(b.date || 0).getTime()) - (new Date(a.date || 0).getTime()));

    const totalCount = window.currentExpenses.length;

    if (totalCount === 0 && initialLoad) {
        const clone = noExpensesEl.cloneNode(true);
        clone.classList.remove('hidden');
        expensesList.appendChild(clone);
        loadMoreBtn?.classList.add('hidden');
        return;
    }
    
    if (initialLoad) expensesList.innerHTML = '';

    noExpensesEl.classList.add('hidden');

    const fragment = document.createDocumentFragment();
    
    const expensesToDisplay = initialLoad ? window.currentExpenses : sortedNewExpenses;

    expensesToDisplay.forEach(exp => {
        const item = document.createElement('div');
        item.className = 'bg-white p-4 rounded-xl shadow-lg flex justify-between items-start border-l-4 border-indigo-500 hover:shadow-xl transition duration-150 mb-3 cursor-pointer';
        item.onclick = () => window.openViewOrEditModal(exp.id, false);

        const dateStr = exp.date ? new Date(exp.date).toLocaleDateString('en-IN') : 'N/A';
        const paidByUsername = exp.paidBy || 'N/A';
        
        let token = localStorage.getItem('jwtToken');
        let isPayer = false;
        if (token) {
            const payload = decodeJwtToken(token);
            const jwtUsername = payload?.username || payload?.sub;
            isPayer = paidByUsername.toLowerCase() === jwtUsername?.toLowerCase();
        }
        
        let actionButton = '';

        if (isPayer) {
            actionButton = `
                <button
                    onclick="event.stopPropagation(); window.openViewOrEditModal('${exp.id}', true)"
                    class="text-xs font-semibold px-3 py-1 rounded-full bg-indigo-100 text-indigo-600 hover:bg-indigo-200 transition"
                >
                    Update
                </button>
            `;
        } else {
             actionButton = `
                <button
                    onclick="event.stopPropagation(); window.openViewOrEditModal('${exp.id}', false)"
                    class="text-xs font-semibold px-3 py-1 rounded-full bg-gray-100 text-gray-600 hover:bg-gray-200 transition"
                >
                    View Details
                </button>
            `;
        }

        item.innerHTML = `
            <div class="flex-grow">
                <p class="font-bold text-xl text-gray-900">${exp.description || 'Expense'}</p>
                <p class="text-sm text-gray-500">Paid by: <span class="font-medium text-indigo-600">${paidByUsername}</span> | Date: ${dateStr}</p>
                ${exp.splits && exp.splits.length > 0 ? 
                    `<div class="mt-2 text-xs text-gray-600">
                        <strong>Split:</strong> ${exp.splits.map(split => 
                            `${split.memberUsername}: ₹${split.owedAmount.toFixed(2)}`
                        ).join(', ')}
                    </div>` : ''
                }
            </div>
            <div class="flex flex-col items-end space-y-2">
                <span class="font-extrabold text-xl text-red-600">-₹${(exp.amount || 0).toFixed(2)}</span>
                ${actionButton}
            </div>
        `;
        fragment.appendChild(item);
    });

    expensesList.appendChild(fragment);

    if (loadMoreBtn) {
        if (hasMoreExpenses) {
            loadMoreBtn.classList.remove('hidden');
        } else {
            loadMoreBtn.classList.add('hidden');
        }
    }
}

window.resetExpenseForm = function() {
    let token = localStorage.getItem('jwtToken');
    if (token) {
        const payload = decodeJwtToken(token);
        const jwtUsername = payload?.username || payload?.sub;
        if (!isUserMemberOfGroup(jwtUsername)) {
            window.alertUserMessage("You must be a member of this group to add expenses.", 'error');
            return;
        }
    }

    window.currentExpenseId = null;
    expenseForm.reset();
    
    if (token) {
        const payload = decodeJwtToken(token);
        const jwtUsername = payload?.username || payload?.sub;
        paidByInput.value = jwtUsername || window.currentUsername;
    } else {
        paidByInput.value = window.currentUsername;
    }
    
    modalTitle.textContent = 'Add New Expense';
    submitExpenseBtn.textContent = 'Add Expense';
    submitExpenseBtn.classList.remove('hidden');
    
    window.setFormEditMode(true); 
    
    window.populateExpenseSplitTable([], true);
    window.handleExpenseFormChanges(); 
    window.openModal('expense-modal');
};

window.openViewOrEditModal = async function(expenseId, isEditMode = false) {
    console.log('🔓 Opening modal for expense:', expenseId, 'Edit mode:', isEditMode);
    
    window.currentExpenseId = expenseId;
    
    let expense = null;
    
    // ALWAYS FETCH FRESH DATA FROM DATABASE
    console.log('🔄 ALWAYS fetching fresh expense data from database...');
    try {
        expense = await window.fetchExpenseById(window.currentGroupId, expenseId);
        if (!expense) {
            throw new Error('Failed to fetch expense data');
        }
        console.log('✅ Successfully loaded fresh expense data from DB');
    } catch (error) {
        console.error('❌ Failed to fetch fresh expense data:', error);
        window.alertUserMessage("Failed to load expense details. Please try again.", 'error');
        return;
    }

    if (!expense) {
        console.error('Expense not found:', expenseId);
        window.alertUserMessage("Expense not found for viewing/editing.", 'error');
        return;
    }
    
    console.log('=== FRESH EXPENSE DATA FROM DB ===');
    console.log('Expense amount:', expense.amount);
    console.log('Expense splits:', expense.splits);
    console.log('Expense splits length:', expense.splits?.length);
    
    if (expense.splits && expense.splits.length > 0) {
        expense.splits.forEach((split, index) => {
            console.log(`Split ${index}: ${split.memberUsername} - Owed: ${split.owedAmount}, Paid: ${split.paidAmount}`);
        });
    } else {
        console.log('No splits found in expense data');
    }
    
    modalTitle.textContent = isEditMode ? 'Update Expense' : 'Expense Details';
    window.setFormEditMode(isEditMode); 

    // Set the form values from FRESH data
    amountInput.value = expense.amount ? expense.amount.toFixed(2) : '0.00';
    descriptionInput.value = expense.description || '';
    paidByInput.value = expense.paidBy || window.currentUsername;
    
    console.log('Form values set from FRESH data - Amount:', amountInput.value, 'Description:', descriptionInput.value, 'PaidBy:', paidByInput.value);
    
    // Use splits from the FRESH expense data
    console.log('Passing FRESH splits to populateExpenseSplitTable:', expense.splits);
    window.populateExpenseSplitTable(expense.splits || [], isEditMode);
    
    if (isEditMode) {
        submitExpenseBtn.textContent = 'Save Changes';
        submitExpenseBtn.classList.remove('hidden');
    } else {
        submitExpenseBtn.classList.add('hidden');
    }

    window.openModal('expense-modal');
};

window.setFormEditMode = function(isEditMode) {
    const inputs = expenseForm.querySelectorAll('input:not(#paidBy), textarea');
    inputs.forEach(input => {
        if (!input.classList.contains('owed-input') && !input.classList.contains('paid-input')) {
            input.readOnly = !isEditMode;
        }
    });
    
    paidByInput.readOnly = true;

    const summaryArea = document.getElementById('split-summary-area'); 
    if (summaryArea) {
        summaryArea.classList.toggle('hidden', !isEditMode);
    }

    if (!isEditMode) {
        inputs.forEach(input => {
            if (input.readOnly) {
                input.classList.add('bg-gray-50', 'text-gray-700', 'cursor-not-allowed');
                input.classList.remove('bg-white', 'text-gray-900');
            }
        });
    } else {
        inputs.forEach(input => {
            if (!input.readOnly) {
                input.classList.remove('bg-gray-50', 'text-gray-700', 'cursor-not-allowed');
                input.classList.add('bg-white', 'text-gray-900');
            }
        });
    }
}

window.populateExpenseSplitTable = function(existingSplits = [], isEditMode = false) {
    if (!splitTableBody) return;
    
    splitTableBody.innerHTML = '';
    
    console.log('=== DEBUG: populateExpenseSplitTable START ===');
    console.log('existingSplits:', existingSplits);
    console.log('existingSplits length:', existingSplits.length);
    console.log('isEditMode:', isEditMode);
    
    const totalAmount = parseFloat(amountInput.value) || 0;
    console.log('Total amount from input:', totalAmount);
    
    const membersList = (window.currentGroupMembers || []).map(m => (typeof m === 'object' && m !== null) ? m.username.toLowerCase() : m.toLowerCase());
    if (!membersList.includes(window.currentUsername.toLowerCase())) {
        membersList.push(window.currentUsername.toLowerCase());
    }
    const uniqueMembers = [...new Set(membersList)];
    
    console.log('Unique members:', uniqueMembers);
    console.log('Current payer:', paidByInput.value.toLowerCase());
    
    const currentPayer = paidByInput.value.toLowerCase();

    uniqueMembers.forEach(username => {
        const isPayer = username === currentPayer;
        
        // Find existing split for this user
        const existingSplit = existingSplits.find(s => {
            const splitUsername = s.memberUsername?.toLowerCase();
            return splitUsername === username.toLowerCase();
        });

        console.log(`Processing user: ${username}, found split:`, existingSplit);

        let owedAmount = 0;
        let paidAmount = 0;

        if (existingSplit) {
            // Use the actual values from database
            owedAmount = typeof existingSplit.owedAmount === 'number' ? 
                existingSplit.owedAmount : 
                parseFloat(existingSplit.owedAmount) || 0;
            
            paidAmount = typeof existingSplit.paidAmount === 'number' ? 
                existingSplit.paidAmount : 
                parseFloat(existingSplit.paidAmount) || 0;
                
            console.log(`✅ USING EXISTING SPLIT - ${username}: Owed: ${owedAmount}, Paid: ${paidAmount}`);
        } else {
            // Only calculate equal split if no existing data
            owedAmount = uniqueMembers.length > 0 && totalAmount > 0 ? totalAmount / uniqueMembers.length : 0;
            paidAmount = isPayer && totalAmount > 0 ? totalAmount : 0;
            console.log(`❌ NO EXISTING SPLIT FOUND - ${username}: Using equal split - Owed: ${owedAmount}, Paid: ${paidAmount}`);
        }
        
        const netBalance = existingSplit?.netBalance !== undefined ? 
            (typeof existingSplit.netBalance === 'number' ? existingSplit.netBalance : parseFloat(existingSplit.netBalance) || 0) :
            roundToTwoDecimals(paidAmount - owedAmount);

        const owedValue = owedAmount.toFixed(2);
        const paidValue = paidAmount.toFixed(2);
        const netValue = netBalance;

        console.log(`Final values for ${username} - Owed: ${owedValue}, Paid: ${paidValue}, Net: ${netValue}`);

        const row = splitTableBody.insertRow();
        row.id = `split-row-${username}`;
        
        row.insertCell().innerHTML = `<span class="font-medium text-gray-800">${username}</span>${isPayer ? ' (payer)' : ''}`;

        if (isEditMode) {
            const owedCell = row.insertCell();
            owedCell.className = "px-1 py-1 text-center";
            owedCell.innerHTML = `<input type="number" step="0.01" value="${owedValue}" data-username="${username}" class="owed-input w-24 text-center border border-gray-300 rounded p-1 text-sm focus:ring-indigo-500 focus:border-indigo-500" oninput="window.handleExpenseFormChanges()">`;

            const paidCell = row.insertCell();
            paidCell.className = "px-1 py-1 text-center";
            
            const paidReadOnly = isPayer ? 'readonly' : '';
            const paidClasses = isPayer ? 'bg-indigo-50 font-bold text-indigo-700 cursor-not-allowed' : '';
            
            paidCell.innerHTML = `<input type="number" step="0.01" value="${paidValue}" data-username="${username}" ${paidReadOnly} 
                class="paid-input w-24 text-center border border-gray-300 rounded p-1 text-sm ${paidClasses}" oninput="window.handleExpenseFormChanges()">`;

        } else {
            row.insertCell().innerHTML = `<span class="text-gray-700 font-medium block w-full text-right py-2 px-2">₹${owedValue}</span>`;
            row.insertCell().innerHTML = `<span class="text-gray-700 font-medium block w-full text-right py-2 px-2 ${isPayer ? 'font-bold text-indigo-700' : ''}">₹${paidValue}</span>`;
        }

        const netCell = row.insertCell();
        netCell.className = "px-2 py-1 text-right";
        
        let netDisplay = '';
        if (netValue > 0.01) {
            netDisplay = `<span class="text-green-600 font-bold">+₹${Math.abs(netValue).toFixed(2)}</span><br><small class="text-green-500">(gets back)</small>`;
        } else if (netValue < -0.01) {
            netDisplay = `<span class="text-red-600 font-bold">-₹${Math.abs(netValue).toFixed(2)}</span><br><small class="text-red-500">(owes)</small>`;
        } else {
            netDisplay = `<span class="text-gray-800 font-bold">₹${netValue.toFixed(2)}</span><br><small class="text-gray-500">(settled)</small>`;
        }
        
        netCell.innerHTML = `<div id="net-${username}" class="net-amount">${netDisplay}</div>`;
    });
    
    console.log('=== DEBUG: populateExpenseSplitTable END ===');
    setTimeout(() => window.handleExpenseFormChanges(), 100);
}

window.handleExpenseFormChanges = function() {
    const totalAmount = parseFloat(amountInput?.value) || 0;
    const currentPayer = paidByInput.value.toLowerCase();
    
    console.log('=== handleExpenseFormChanges ===');
    console.log('Total amount:', totalAmount);
    console.log('Current payer:', currentPayer);
    
    if (!amountInput.readOnly) {
        const payerPaidInput = document.querySelector(`.paid-input[data-username="${currentPayer}"]`);
        if (payerPaidInput) {
            payerPaidInput.value = totalAmount.toFixed(2);
            payerPaidInput.readOnly = true; 
            console.log('Set payer paid input to:', payerPaidInput.value);
        }
    }

    if (totalAmount < 0.01) {
        console.log('Total amount is zero or very small');
        if (owedTotalSummaryEl) owedTotalSummaryEl.textContent = `₹0.00`;
        if (remainingAmountEl) remainingAmountEl.textContent = `₹0.00`;
        if (submitExpenseBtn) submitExpenseBtn.disabled = true;
        return;
    }

    let sumOfOwed = 0;
    let sumOfPaid = 0;
    const isCloseToZero = (value) => Math.abs(value) < 0.01;

    document.querySelectorAll('#expense-split-table-body tr').forEach(row => {
        const owedInput = row.querySelector('.owed-input');
        const paidInput = row.querySelector('.paid-input');
        const netDiv = row.querySelector('.net-amount');
        
        let owed = 0;
        let paid = 0;
        
        if (owedInput) {
            owed = parseFloat(owedInput.value) || 0;
        } else {
            const owedSpan = row.querySelector('td:nth-child(2) span');
            if (owedSpan) {
                const owedText = owedSpan.textContent.replace('₹', '');
                owed = parseFloat(owedText) || 0;
            }
        }
        
        if (paidInput) {
            paid = parseFloat(paidInput.value) || 0;
        } else {
            const paidSpan = row.querySelector('td:nth-child(3) span');
            if (paidSpan) {
                const paidText = paidSpan.textContent.replace('₹', '');
                paid = parseFloat(paidText) || 0;
            }
        }
        
        console.log(`Row ${row.id}: Owed=${owed}, Paid=${paid}`);
        
        sumOfOwed = roundToTwoDecimals(sumOfOwed + owed);
        sumOfPaid = roundToTwoDecimals(sumOfPaid + paid);

        const net = roundToTwoDecimals(paid - owed);
        
        if (netDiv) {
            let netDisplay = '';
            if (net > 0.01) {
                netDisplay = `<span class="text-green-600 font-bold">+₹${Math.abs(net).toFixed(2)}</span><br><small class="text-green-500">(gets back)</small>`;
            } else if (net < -0.01) {
                netDisplay = `<span class="text-red-600 font-bold">-₹${Math.abs(net).toFixed(2)}</span><br><small class="text-red-500">(owes)</small>`;
            } else {
                netDisplay = `<span class="text-gray-800 font-bold">₹${net.toFixed(2)}</span><br><small class="text-gray-500">(settled)</small>`;
            }
            netDiv.innerHTML = netDisplay;
        }
    });

    console.log(`Final sums - Owed: ${sumOfOwed}, Paid: ${sumOfPaid}`);

    const owedRemaining = roundToTwoDecimals(totalAmount - sumOfOwed);
    
    if (owedTotalSummaryEl) owedTotalSummaryEl.textContent = `₹${sumOfOwed.toFixed(2)}`;
    
    const isValidOwedSplit = isCloseToZero(owedRemaining);
    const isValid = isValidOwedSplit; 

    if (remainingAmountEl) {
        remainingAmountEl.classList.remove('text-red-600', 'text-green-600');
        if (!isValidOwedSplit) {
            remainingAmountEl.textContent = `₹${Math.abs(owedRemaining).toFixed(2)} ${owedRemaining > 0 ? 'Remaining' : 'Over Assigned'}`;
            remainingAmountEl.classList.add('text-red-600');
        } else {
            remainingAmountEl.textContent = `Balanced! (₹${totalAmount.toFixed(2)} Split)`;
            remainingAmountEl.classList.add('text-green-600');
        }
    }
    
    if (submitExpenseBtn && !amountInput.readOnly) {
        submitExpenseBtn.disabled = !isValid;
    }
};

// ----------------------------------------------------------------------------------
// --- EXPENSE SUBMISSION ---
// ----------------------------------------------------------------------------------

async function handleSubmitExpense(event) {
    event.preventDefault();
    
    console.log('=== DEBUG: Submitting expense ===');
    console.log('Current expense ID:', window.currentExpenseId);
    console.log('Current group ID:', window.currentGroupId);
    
    const validation = await validateTokenAndGroupMembership(window.currentExpenseId);
    if (!validation) return;
    
    const { token, jwtUsername } = validation;
    
    console.log('Validated user:', jwtUsername);
    
    window.handleExpenseFormChanges();
    if (submitExpenseBtn.disabled) {
        window.alertUserMessage("Please ensure Total Owed is balanced with the Total Amount.", 'warning');
        return; 
    }

    const amount = parseFloat(amountInput.value);
    const description = descriptionInput.value.trim();
    const paidBy = paidByInput.value;
    
    console.log('Form data - Amount:', amount, 'Description:', description, 'PaidBy:', paidBy);
    
    const splitDetails = [];
    document.querySelectorAll('#expense-split-table-body tr').forEach(row => {
        const username = row.id.replace('split-row-', '');
        
        const owedInput = row.querySelector('.owed-input');
        const paidInput = row.querySelector('.paid-input');
        
        let owed = 0;
        let paid = 0;
        
        if (owedInput) {
            owed = parseFloat(owedInput.value) || 0;
        } else {
            const owedSpan = row.querySelector('td:nth-child(2) span');
            if (owedSpan) {
                const owedText = owedSpan.textContent.replace('₹', '');
                owed = parseFloat(owedText) || 0;
            }
        }
        
        if (paidInput) {
            paid = parseFloat(paidInput.value) || 0;
        } else {
            const paidSpan = row.querySelector('td:nth-child(3) span');
            if (paidSpan) {
                const paidText = paidSpan.textContent.replace('₹', '');
                paid = parseFloat(paidText) || 0;
            }
        }
        
        if (owed > 0.00 || paid > 0.00) {
            splitDetails.push({
                memberUsername: username,
                owedAmount: roundToTwoDecimals(owed),
                paidAmount: roundToTwoDecimals(paid)
            });
        }
    });

    console.log('Split details:', splitDetails);

    const expenseData = { 
        amount: roundToTwoDecimals(amount), 
        description, 
        paidBy: jwtUsername,
        splitDetails
    };

    let method = 'POST';
    let url = `${window.apiUrlBase}${window.currentGroupId}/expenses`;

    if (window.currentExpenseId) {
        method = 'PUT';
        url = `${url}/${window.currentExpenseId}`;
        console.log('PUT URL:', url);
    }

    console.log('Final request details:', { 
        method, 
        url, 
        expenseData,
        groupId: window.currentGroupId,
        expenseId: window.currentExpenseId,
        jwtUsername
    });

    submitExpenseBtn.disabled = true;
    submitExpenseBtn.textContent = 'Saving...';

    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(expenseData)
        });

        console.log('Response status:', response.status);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Full error response:', errorText);
            
            let errorMessage = `Failed to save expense: ${response.status} - ${response.statusText}`;
            
            if (response.status === 403) {
                errorMessage = "Access denied. ";
                
                try {
                    const errorJson = JSON.parse(errorText);
                    const backendMessage = errorJson.message || errorJson.error || '';
                    errorMessage += backendMessage;
                    console.error('Backend error details:', errorJson);
                } catch (e) {
                    console.error('Raw backend response:', errorText);
                    errorMessage += "You don't have permission to perform this action. Please ensure you're a member of this group.";
                }
                
                window.alertUserMessage(errorMessage, 'error');
                return;
                
            } else if (response.status === 401) {
                errorMessage = "Authentication failed. Please log in again.";
                handleAuthError(errorMessage);
                return;
            } else {
                try {
                    const errorJson = JSON.parse(errorText);
                    errorMessage = errorJson.message || errorJson.error || errorMessage;
                    console.error('Parsed error JSON:', errorJson);
                } catch (e) {
                    console.error('Raw error text:', errorText);
                    errorMessage += ` - ${errorText.substring(0, 200)}`;
                }
            }
            
            throw new Error(errorMessage);
        }

        const responseData = await response.json();
        console.log('Success response:', responseData);

        window.alertUserMessage(`Expense successfully ${window.currentExpenseId ? 'updated' : 'added'}!`, 'success');
        window.closeModal('expense-modal');

        currentPage = 0;
        hasMoreExpenses = true;
        window.currentExpenses = [];
        window.fetchExpensesPage(window.currentGroupId, currentPage, true);

    } catch (error) {
        console.error('Submission error:', error);
        if (!error.message.includes('Access denied')) {
            window.handleError(`Expense submission error: ${error.message}`);
        }
    } finally {
        submitExpenseBtn.disabled = false;
        submitExpenseBtn.textContent = window.currentExpenseId ? 'Save Changes' : 'Add Expense';
    }
}

// ----------------------------------------------------------------------------------
// --- INITIALIZATION AND EVENT LISTENERS ---
// ----------------------------------------------------------------------------------

document.addEventListener('DOMContentLoaded', () => {
    if (expenseForm) {
        expenseForm.addEventListener('submit', handleSubmitExpense);
    }

    amountInput?.addEventListener('input', () => {
        if (!amountInput.readOnly) {
             window.populateExpenseSplitTable([], true); 
             window.handleExpenseFormChanges();
        }
    });
    
    document.getElementById('add-expense-btn')?.addEventListener('click', window.resetExpenseForm);

    loadMoreBtn?.addEventListener('click', () => {
        window.fetchExpensesPage(window.currentGroupId, currentPage + 1, false);
    });

    if (window.currentGroupId) {
        currentPage = 0;
        hasMoreExpenses = true;
        window.currentExpenses = []; 
        window.fetchExpensesPage(window.currentGroupId, currentPage, true);
    } else {
         const checkIdInterval = setInterval(() => {
             if (window.currentGroupId) {
                 clearInterval(checkIdInterval);
                 currentPage = 0;
                 hasMoreExpenses = true;
                 window.currentExpenses = [];
                 window.fetchExpensesPage(window.currentGroupId, currentPage, true);
             }
         }, 100); 
    }
});