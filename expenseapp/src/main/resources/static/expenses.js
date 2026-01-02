// Global variables to manage state (assuming these are defined in your main HTML file)
// window.currentUsername = "username1";
// window.currentGroupId = "group-123";
// window.currentGroupMembers = [{username: "user1"}, {username: "user2"}];
// window.currentExpenses = []; // List of all expenses for the current group
window.currentExpenseId = null; // Tracks which expense is currently being edited

// --- PAGINATION STATE ---
window.currentPage = 1; 
window.expensesPerPage = 5; // Default number of expenses to show per page

// Helper function to handle rounding to two decimal places for currency
const roundToTwoDecimals = (num) => Math.round(num * 100) / 100;

// -----------------------------------------------------------------------------
// EXPENSE RENDERING AND PAGINATION
// -----------------------------------------------------------------------------

/** * Renders the list of expenses with an Update button, now with pagination. (Currency: ₹) 
 * NOTE: This function now accepts the full list of expenses and paginates it.
 */
window.renderExpenses = function(expenses) {
    const list = document.getElementById('expenses-list');
    const noExpensesEl = document.getElementById('no-expenses');
    const paginationControls = document.getElementById('pagination-controls');
    const pageInfoEl = document.getElementById('page-info');
    
    // CRITICAL: If these elements are not in your HTML, nothing will work.
    if (!list || !noExpensesEl || !paginationControls || !pageInfoEl) {
        console.error("Missing required HTML elements: #expenses-list, #no-expenses, #pagination-controls, or #page-info.");
        return;
    }

    // Store the fetched expenses globally for use in the update function
    window.currentExpenses = expenses || []; 

    list.innerHTML = '';
    
    if (!expenses || expenses.length === 0) {
        noExpensesEl.classList.remove('hidden');
        list.appendChild(noExpensesEl);
        paginationControls.classList.add('hidden'); // Hide pagination when no expenses
        return;
    }
    noExpensesEl.classList.add('hidden');

    const sortedExpenses = expenses.sort((a, b) => (new Date(b.date || 0).getTime()) - (new Date(a.date || 0).getTime()));
    
    // --- PAGINATION LOGIC ---
    const totalExpenses = sortedExpenses.length;
    const totalPages = Math.ceil(totalExpenses / window.expensesPerPage);

    // Keep the current page within bounds
    if (window.currentPage > totalPages) {
        window.currentPage = totalPages;
    } else if (window.currentPage < 1) {
        window.currentPage = 1;
    }

    const startIndex = (window.currentPage - 1) * window.expensesPerPage;
    const endIndex = startIndex + window.expensesPerPage;
    const expensesToRender = sortedExpenses.slice(startIndex, endIndex);

    // Render the actual list items
    expensesToRender.forEach(exp => {
        const item = document.createElement('div');
        item.className = 'bg-white p-4 rounded-xl shadow-lg flex justify-between items-start border-l-4 border-indigo-500 hover:shadow-xl transition duration-150 mb-3';
        
        const dateStr = exp.date ? new Date(exp.date).toLocaleDateString('en-IN') : 'N/A';
        const paidByUsername = exp.paidBy || 'N/A';
        
        item.innerHTML = `
            <div class="flex-grow">
                <p class="font-bold text-xl text-gray-900">${exp.description || 'Expense'}</p>
                <p class="text-sm text-gray-500">Paid by: <span class="font-medium text-indigo-600">${paidByUsername}</span> | Date: ${dateStr}</p>
            </div>
            <div class="flex flex-col items-end space-y-2">
                <span class="font-extrabold text-xl text-red-600">-₹${(exp.amount || 0).toFixed(2)}</span>
                <button 
                    onclick="window.openEditModal(${exp.id})" 
                    class="text-xs font-semibold px-3 py-1 rounded-full bg-indigo-100 text-indigo-600 hover:bg-indigo-200 transition"
                >
                    Update
                </button>
            </div>
        `;
        list.appendChild(item);
    });
    
    // Update pagination controls display
    paginationControls.classList.remove('hidden');
    pageInfoEl.textContent = `Page ${window.currentPage} of ${totalPages} (${totalExpenses} total expenses)`;
    
    document.getElementById('prev-page-btn').disabled = window.currentPage === 1;
    document.getElementById('next-page-btn').disabled = window.currentPage === totalPages;
}

/** * Handles changing the page. This function should be called by the pagination controls.
 * @param {number} direction - 1 for next, -1 for previous.
 */
window.changePage = function(direction) {
    const totalPages = Math.ceil(window.currentExpenses.length / window.expensesPerPage);
    const newPage = window.currentPage + direction;

    if (newPage >= 1 && newPage <= totalPages) {
        window.currentPage = newPage;
        // Re-render the expenses using the stored full list
        window.renderExpenses(window.currentExpenses);
    }
}

// -----------------------------------------------------------------------------
// EXPENSE FORM LOGIC
// -----------------------------------------------------------------------------

/** Prepares the form for editing an existing expense. */
window.openEditModal = function(expenseId) {
    const expense = window.currentExpenses.find(e => e.id === expenseId);
    if (!expense) {
        window.handleError(`Expense with ID ${expenseId} not found for editing.`);
        return;
    }

    // Set state to EDIT mode
    window.currentExpenseId = expenseId;
    
    // Update modal elements
    document.getElementById('expense-modal-title').textContent = 'Update Expense';
    document.getElementById('submit-expense-btn').textContent = 'Save Changes';

    window.loadExpenseForEdit(expense);
    // Assuming window.openModal exists and opens the expense form modal
    window.openModal('expense-modal'); 
}

/** Loads existing expense data into the form fields. */
window.loadExpenseForEdit = function(expense) {
    const form = document.getElementById('add-expense-form');
    form.amount.value = (expense.amount || 0).toFixed(2);
    form.description.value = expense.description || '';
    
    window.populateExpenseSplitTable(); 
    
    expense.splits.forEach(split => {
        const username = split.memberUsername.toLowerCase();
        const owedInput = document.querySelector(`.owed-input[data-username="${username}"]`);
        const paidInput = document.querySelector(`.paid-input[data-username="${username}"]`);

        if (owedInput) {
            owedInput.value = (split.owedAmount || 0).toFixed(2);
        }
        if (paidInput) {
            paidInput.value = (split.paidAmount || 0).toFixed(2);
        }
    });

    window.handleExpenseFormChanges(); 
}

/** Resets the expense form for adding a new expense. */
window.resetExpenseForm = function() {
    window.currentExpenseId = null; // Set state to ADD mode
    const form = document.getElementById('add-expense-form');
    
    form.reset();
    
    document.getElementById('expense-modal-title').textContent = 'Add New Expense';
    document.getElementById('submit-expense-btn').textContent = 'Submit Expense';

    window.populateExpenseSplitTable();
    window.handleExpenseFormChanges(); 
}


/** Renders the table rows for expense splitting, allowing unequal splits. */
window.populateExpenseSplitTable = function() {
    const tableBody = document.getElementById('expense-split-table-body');
    if (!tableBody) return;

    tableBody.innerHTML = '';
    
    const totalAmount = parseFloat(document.getElementById('amount')?.value) || 0;
    
    const membersList = window.currentGroupMembers.map(m => (typeof m === 'object' && m !== null) ? m.username.toLowerCase() : m.toLowerCase());
    if (!membersList.includes(window.currentUsername.toLowerCase())) {
        membersList.push(window.currentUsername.toLowerCase());
    }
    const uniqueMembers = [...new Set(membersList)];
    
    const initialSplit = uniqueMembers.length > 0 ? totalAmount / uniqueMembers.length : 0;


    uniqueMembers.forEach(username => {
        const isPayer = username.toLowerCase() === window.currentUsername.toLowerCase();

        const row = tableBody.insertRow();
        row.id = `split-row-${username}`;
        
        // Member Name Column
        row.insertCell().innerHTML = `<span class="font-medium">${username}</span>${isPayer ? ' (You)' : ''}`;

        // Owed Column
        const owedInput = document.createElement('input');
        owedInput.type = 'number';
        owedInput.step = '0.01';
        owedInput.value = initialSplit.toFixed(2); 
        owedInput.className = `owed-input w-24 text-center border rounded p-1 text-sm`;
        owedInput.dataset.username = username;
        owedInput.dataset.type = 'owed';
        owedInput.oninput = window.handleExpenseFormChanges; 
        row.insertCell().appendChild(owedInput);

        // Paid Column
        const paidInput = document.createElement('input');
        paidInput.type = 'number';
        paidInput.step = '0.01';
        paidInput.value = isPayer ? totalAmount.toFixed(2) : '0.00'; 
        paidInput.className = `paid-input w-24 text-center border rounded p-1 text-sm ${isPayer ? 'bg-indigo-50 font-bold cursor-default' : ''}`;
        paidInput.dataset.username = username;
        paidInput.dataset.type = 'paid';
        
        paidInput.readOnly = isPayer; 
        paidInput.oninput = window.handleExpenseFormChanges; 
        
        row.insertCell().appendChild(paidInput);

        // Net Column
        row.insertCell().innerHTML = `<span id="net-${username}" class="net-amount font-bold">₹0.00</span>`;
    });
}


/** Handles all form changes (Total Amount, Paid inputs, Owed inputs) and updates calculations. */
window.handleExpenseFormChanges = function() {
    const totalAmountInput = document.getElementById('amount');
    let totalAmount = parseFloat(totalAmountInput?.value) || 0;
    const submitBtn = document.getElementById('submit-expense-btn');

    const payerPaidInput = document.querySelector(`.paid-input[data-username="${window.currentUsername.toLowerCase()}"]`);
    if (payerPaidInput && payerPaidInput.readOnly) {
        payerPaidInput.value = totalAmount.toFixed(2);
    }
    
    if (totalAmount < 0.01) {
        document.getElementById('owed-total-summary').textContent = `₹0.00`;
        document.getElementById('remaining-amount').textContent = `₹0.00`;
        if (submitBtn) submitBtn.disabled = true;
        return;
    }

    let sumOfOwed = 0;
    let sumOfPaid = 0;

    // 1. Loop through all split rows to read user input and update Net
    document.querySelectorAll('#expense-split-table-body tr').forEach(row => {
        
        const owedInput = row.querySelector('.owed-input');
        const paidInput = row.querySelector('.paid-input');
        const netSpan = row.querySelector('.net-amount');
        
        const owed = parseFloat(owedInput?.value) || 0;
        const paid = parseFloat(paidInput?.value) || 0;
        
        sumOfOwed = roundToTwoDecimals(sumOfOwed + owed);
        sumOfPaid = roundToTwoDecimals(sumOfPaid + paid);

        const net = paid - owed;
        
        // Update Net column (Currency: ₹)
        if (netSpan) {
            netSpan.textContent = `₹${net.toFixed(2)}`;
            netSpan.classList.remove('text-green-600', 'text-red-600', 'text-gray-800');
            if (Math.abs(net) < 0.01) {
                netSpan.classList.add('text-gray-800');
            } else if (net > 0) {
                netSpan.classList.add('text-green-600'); // Will get back
            } else {
                netSpan.classList.add('text-red-600'); // Owes money
            }
        }
    });

    // 2. Update Summary
    const owedRemaining = roundToTwoDecimals(totalAmount - sumOfOwed);

    document.getElementById('owed-total-summary').textContent = `₹${sumOfOwed.toFixed(2)}`;
    
    const remainingEl = document.getElementById('remaining-amount');
    remainingEl.textContent = `₹${owedRemaining.toFixed(2)}`;
    
    // 3. Validate and Enable/Disable Submit Button
    const isCloseToZero = (value) => Math.abs(value) < 0.01;

    const isValidOwedSplit = isCloseToZero(owedRemaining);
    const isValid = isValidOwedSplit; 

    // Update UI for Owed Remaining validation
    if (!isValidOwedSplit) {
        remainingEl.classList.add('text-red-600');
        remainingEl.classList.remove('text-green-600');
    } else {
        remainingEl.classList.remove('text-red-600');
        remainingEl.classList.add('text-green-600');
    }
    
    if (submitBtn) {
        if (isValid) {
            submitBtn.disabled = false;
        } else {
            submitBtn.disabled = true;
        }
    }
}


/** Handles the submission (Add or Update) of an expense. */
async function handleSubmitExpense(event) {
    event.preventDefault();
    let token = localStorage.getItem('jwtToken');
    
    if (!window.currentGroupId || !token) {
        window.handleError("Error: Group or authentication token missing."); 
        return;
    }

    const form = event.target;
    const submitBtn = document.getElementById('submit-expense-btn');

    window.handleExpenseFormChanges();
    if (submitBtn.disabled) {
        window.alertUserMessage("Please ensure Total Owed matches the Total Amount.", 'warning');
        return; 
    }
    
    // --- Data Collection ---
    const totalAmount = parseFloat(form.amount.value);
    const splitDetails = [];
    document.querySelectorAll('#expense-split-table-body tr').forEach(row => {
        const username = row.id.replace('split-row-', '');
        const owed = parseFloat(row.querySelector('.owed-input').value) || 0;
        const paid = parseFloat(row.querySelector('.paid-input').value) || 0;
        
        if (owed > 0.00 || paid > 0.00) {
            splitDetails.push({
                memberUsername: username,
                owedAmount: roundToTwoDecimals(owed),
                paidAmount: roundToTwoDecimals(paid)
            });
        }
    });
    
    const expenseData = {
        amount: roundToTwoDecimals(totalAmount),
        description: form.description.value.trim(),
        paidBy: window.currentUsername.toLowerCase(), 
        splitDetails: splitDetails 
    };
    
    // --- API Configuration ---
    let method = 'POST';
    let url = `${window.apiUrlBase}${window.currentGroupId}/expenses`;
    let successMessage = 'Expense submitted successfully.';

    if (window.currentExpenseId) {
        method = 'PUT';
        url = `${window.apiUrlBase}${window.currentGroupId}/expenses/${window.currentExpenseId}`;
        successMessage = 'Expense updated successfully.';
    }

    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = (method === 'POST' ? 'Submitting...' : 'Saving...');
    }

    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}` 
            },
            body: JSON.stringify(expenseData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            let errorMessage = `HTTP error! Status: ${response.status}.`;
            if (response.status === 403) {
                 errorMessage += ` **Forbidden:** You may not be authorized to modify this expense.`;
            } else if (response.status === 400) {
                 errorMessage += ` **Bad Request:** Check if the expense amount is valid or if split details are complete/balanced.`;
            }
            throw new Error(errorMessage + ` Server message: ${errorText.substring(0, Math.min(errorText.length, 100))}...`);
        }

        // Assuming window.closeModal exists
        window.closeModal('expense-modal');
        window.alertUserMessage(`${successMessage} Refreshing data...`); 
        
        // --- Data Refresh and Display ---
        window.currentPage = 1; 
        // CRITICAL: This line assumes a function exists to fetch data.
        await window.fetchGroupDetails(window.currentGroupId); 
        // This line assumes a function exists to switch to the expenses view.
        window.switchTab('expenses'); 

    } catch (error) {
        window.handleError(`Failed to process expense: ${error.message}.`);
    } finally {
        window.resetExpenseForm(); 
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Submit Expense';
        }
    }
}

// -----------------------------------------------------------------------------
// EVENT LISTENERS AND INITIALIZATION
// -----------------------------------------------------------------------------

// Attach event listener when the module loads
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('add-expense-form');
    if (form) {
        form.addEventListener('submit', handleSubmitExpense);
    }
    
    document.getElementById('amount')?.addEventListener('input', () => {
        window.populateExpenseSplitTable(); 
        window.handleExpenseFormChanges();
    });

    // Ensure the form is properly reset when the modal is opened for 'Add New Expense'
    // CRITICAL: Ensure the element with this selector exists in your HTML
    document.querySelector('[data-modal-target="expense-modal"]')?.addEventListener('click', window.resetExpenseForm);
    
    // --- PAGINATION BUTTON LISTENERS ---
    // CRITICAL: Ensure the elements with these IDs exist in your HTML
    document.getElementById('prev-page-btn')?.addEventListener('click', () => window.changePage(-1));
    document.getElementById('next-page-btn')?.addEventListener('click', () => window.changePage(1));
});

