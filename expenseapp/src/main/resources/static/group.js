// group.js

// --- Global Configuration and State (Exported for other modules) ---
export const apiUrlBase = '/api/groups/'; 
export let currentGroupId = null;
export let currentUsername = "CurrentUser";
export let currentGroupMembers = []; 

// --- UI elements (Also exported or attached to window for global access)
export const loadingEl = document.getElementById('loading');
export const errorEl = document.getElementById('error-message');
export const errorDetailsEl = document.getElementById('error-details');
export const tabButtons = document.querySelectorAll('.tab-button');
export const tabPanes = document.querySelectorAll('.tab-pane');
export const userMessageEl = document.getElementById('user-message-box');
export const paidByInput = document.getElementById('paidBy');


// --- Utility Functions (Exported) ---

// JWT & Auth
function decodeJwtUsername(jwtToken) { /* ... (Original decode logic) ... */ }
export function checkAuthAndGetUsername() { 
    // ... (Original auth check logic) ...
    // Must update the exported variables
    const token = localStorage.getItem('jwtToken');
    // ... logic ...
    currentUsername = username.toLowerCase();
    if (paidByInput) paidByInput.value = currentUsername;
    return token;
}

// UI Utilities
export function alertUserMessage(message, type = 'success') { /* ... (Original alert logic) ... */ }
export function handleError(message) { 
    if (loadingEl) loadingEl.classList.add('hidden');
    if (errorEl && errorDetailsEl) {
        errorDetailsEl.textContent = message;
        errorEl.classList.remove('hidden');
    }
    alertUserMessage(message, 'error');
}
export function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('hidden');
        // Call the setup function exported by expenses.js
        if (modalId === 'expense-modal' && window.setupExpenseModal) {
            window.setupExpenseModal(); 
        }
    }
}
export function closeModal(modalId) { 
    // ... (Original closeModal logic) ... 
}

// URL Utilities
function getQueryParam(name) { /* ... (Original param logic) ... */ }
function extractGroupIdFromSlug(slug) { /* ... (Original ID extraction logic) ... */ }


// --- Group Fetching and Rendering ---

function renderGroup(group) { 
    // ... (Original render group details) ... 
    if (window.fetchExpensesPage) window.fetchExpensesPage(group.id, 0, true); // <--- CALL EXPENSE FETCH
    renderMembers(group.members || []);
}

function renderMembers(members) { /* ... (Original render members logic) ... */ }

export async function fetchGroupDetails(groupId) {
    let token = localStorage.getItem('jwtToken');
    if (!token) return handleError('Authentication Failed: No JWT token found.');

    currentGroupId = groupId; // Set the global state here
    if (loadingEl) loadingEl.classList.remove('hidden');

    try {
        // ... (Original fetch logic) ...
        const response = await fetch(apiUrlBase + groupId, { /* ... */ });
        const groupData = await response.json();
        
        currentGroupMembers = groupData.members || []; // Update global members list
        renderGroup(groupData);
    } catch (error) {
        handleError(`Failed to fetch group details. Error: ${error.message}`);
    } finally {
        if (loadingEl) loadingEl.classList.add('hidden');
    }
}

// --- Event Handlers (Member, Tab) ---
export function switchTab(tabId) { 
    // ... (Original switchTab logic) ... 
    // Check for external fetch functions
    if (tabId === 'announcements' && window.fetchAnnouncements) {
        window.fetchAnnouncements(currentGroupId);
    }
    if (tabId === 'expenses' && window.fetchExpensesPage) {
        // Start the paginated expense load (page 0)
        window.fetchExpensesPage(currentGroupId, 0, true);
    }
}

async function handleAddMember(event) { /* ... (Original handleAddMember logic) ... */ }

// --- INITIALIZE ---
document.addEventListener('DOMContentLoaded', () => {
    checkAuthAndGetUsername(); 
    const urlSlug = getQueryParam('slug'); 

    if (urlSlug) {
        const groupId = extractGroupIdFromSlug(urlSlug);
        if (groupId) {
            fetchGroupDetails(groupId);
        } else {
            handleError("Invalid group slug format. Cannot find Group ID in URL.");
        }
    } else {
        handleError("Could not find group slug in the URL.");
    }

    // Attach all listeners
    tabButtons.forEach(button => button.addEventListener('click', (e) => switchTab(e.target.dataset.tab)));
    document.getElementById('add-expense-btn')?.addEventListener('click', () => openModal('expense-modal'));
    document.getElementById('add-member-btn')?.addEventListener('click', () => openModal('member-modal'));
    document.getElementById('add-announcement-btn')?.addEventListener('click', () => openModal('announcement-modal'));
    document.getElementById('add-member-form')?.addEventListener('submit', handleAddMember);
});