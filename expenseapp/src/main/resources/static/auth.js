// auth.js - Common authentication utilities

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
    try {
        const payload = decodeJwtToken(token);
        if (!payload || !payload.exp) return true;
        
        const expiry = payload.exp * 1000;
        const now = Date.now();
        return now >= expiry;
    } catch {
        return true;
    }
}

function checkAuthAndRedirect() {
    const token = localStorage.getItem('jwtToken');
    
    if (!token) {
        console.log("No JWT token found. Redirecting to login...");
        window.location.href = 'login.html';
        return false;
    }
    
    if (isTokenExpired(token)) {
        console.log("JWT token expired. Redirecting to login...");
        localStorage.removeItem('jwtToken');
        window.location.href = 'login.html';
        return false;
    }
    
    return true;
}

function navigateToDashboard() {
    if (checkAuthAndRedirect()) {
        window.location.href = 'homepage.html';
    }
}

function getCurrentUsername() {
    const token = localStorage.getItem('jwtToken');
    if (!token) return null;
    
    const payload = decodeJwtToken(token);
    return payload?.username || payload?.sub || null;
}

function logout() {
    localStorage.removeItem('jwtToken');
    window.location.href = 'login.html';
}

// Auto-check auth on protected pages
document.addEventListener('DOMContentLoaded', function() {
    // Check if we're on a protected page (not login/register)
    const protectedPages = ['homepage.html', 'create-group.html', 'manage-groups.html', 'personal-expenses.html', 'group-details.html'];
    const currentPage = window.location.pathname.split('/').pop();
    
    if (protectedPages.includes(currentPage)) {
        if (!checkAuthAndRedirect()) {
            return;
        }
    }
});