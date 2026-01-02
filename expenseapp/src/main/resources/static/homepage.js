// --- JWT HANDLING LOGIC ---

function decodeJwtUsername(token) {
    if (!token) return null;
    try {
        const parts = token.split('.');
        if (parts.length !== 3) return null;

        const payload = parts[1];
        const decodedPayload = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
        const data = JSON.parse(decodedPayload);
        console.log("Decoded JWT payload:", data);
        return data.username || data.sub || data.name || null;
    } catch (e) {
        console.error("Failed to decode JWT:", e);
        return null;
    }
}

function decodeJwtFull(token) {
    if (!token) return null;
    try {
        const parts = token.split('.');
        if (parts.length !== 3) return null;

        const payload = parts[1];
        const decodedPayload = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
        return JSON.parse(decodedPayload);
    } catch (e) {
        console.error("Failed to decode JWT:", e);
        return null;
    }
}

function isTokenExpired(token) {
    try {
        const payload = decodeJwtFull(token);
        if (!payload || !payload.exp) return true;

        const expiry = payload.exp * 1000; // Convert to milliseconds
        const now = Date.now();
        return now >= expiry;
    } catch {
        return true;
    }
}

/**
 * Handles user logout by clearing the token and redirecting.
 */
function logout() {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('currentUser');
    alert('You have been logged out.');
    window.location.href = 'login.html';
}

/**
 * Checks for the JWT, decodes the username, and displays it.
 * Redirects to login.html if the token is missing or invalid.
 */
function loadUsername() {
    const token = localStorage.getItem('jwtToken');
    const currentUsernameDisplay = document.getElementById('currentUsernameDisplay');

    if (!token) {
        console.log("No JWT token found. Redirecting to login...");
        window.location.href = 'login.html';
        return;
    }

    // Check if token is expired
    if (isTokenExpired(token)) {
        console.log("JWT token expired. Redirecting to login...");
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('currentUser');
        alert('Your session has expired. Please login again.');
        window.location.href = 'login.html';
        return;
    }

    const username = decodeJwtUsername(token);

    if (username) {
        currentUsernameDisplay.textContent = username;
        
        // Store user info for later use
        const userData = decodeJwtFull(token);
        localStorage.setItem('currentUser', JSON.stringify({
            username: username,
            email: userData.email || '',
            name: userData.name || username
        }));
    } else {
        console.log("Invalid JWT token found. Redirecting to login...");
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('currentUser');
        alert('Invalid session. Please login again.');
        window.location.href = 'login.html';
    }
}

// Initialize the username display when the page loads
document.addEventListener('DOMContentLoaded', loadUsername);

// Add keyboard navigation support
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        logout();
    }
});

// Add page transition effects
document.addEventListener('DOMContentLoaded', function() {
    // Add fade-in animation to page
    document.body.style.opacity = '0';
    document.body.style.transition = 'opacity 0.3s ease-in';
    
    setTimeout(() => {
        document.body.style.opacity = '1';
    }, 100);
});

// Prevent accidental page reload/navigation
window.addEventListener('beforeunload', function(e) {
    // You can add confirmation logic here if needed
    // e.preventDefault();
    // e.returnValue = '';
});

// Auto-refresh token check every 5 minutes
setInterval(() => {
    const token = localStorage.getItem('jwtToken');
    if (token && isTokenExpired(token)) {
        console.log("Token expired. Redirecting to login...");
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('currentUser');
        alert('Your session has expired. Please login again.');
        window.location.href = 'login.html';
    }
}, 5 * 60 * 1000); // Check every 5 minutes