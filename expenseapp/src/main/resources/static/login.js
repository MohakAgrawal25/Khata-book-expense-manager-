// Immediately enable the button
document.addEventListener('DOMContentLoaded', () => {
     const loginButton = document.getElementById('loginButton');
     loginButton.querySelector('.button-text').textContent = 'Login';
     loginButton.disabled = false;
});

/**
 * Decodes the JWT payload to extract the username.
 */
function decodeJwtForUsername(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        const payload = JSON.parse(jsonPayload);
        return payload.username || 'User';
    } catch (e) {
        console.error("Failed to decode JWT:", e);
        return 'User';
    }
}

/**
 * Displays a status message to the user.
 */
function showMessage(message, type) {
    const messageBox = document.getElementById('statusMessage');
    messageBox.textContent = message;
    messageBox.classList.remove('hidden', 'bg-red-100', 'text-red-800', 'bg-green-100', 'text-green-800');

    if (type === 'error') {
        messageBox.classList.add('bg-red-100', 'text-red-800');
    } else if (type === 'success') {
        messageBox.classList.add('bg-green-100', 'text-green-800');
    }
    messageBox.classList.remove('hidden');
}

/**
 * Shows loading state on button
 */
function setLoading(loading) {
    const loginButton = document.getElementById('loginButton');
    const buttonText = loginButton.querySelector('.button-text');
    const buttonLoader = loginButton.querySelector('.button-loader');
    
    if (loading) {
        loginButton.classList.add('loading');
        loginButton.disabled = true;
    } else {
        loginButton.classList.remove('loading');
        loginButton.disabled = false;
    }
}

/**
 * Handles the form submission by calling the Spring Boot AuthController endpoint.
 */
window.handleLogin = async function(event) {
    event.preventDefault();

    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const loginButton = document.getElementById('loginButton');
    
    const username = usernameInput.value.trim();
    const password = passwordInput.value;
    
    if (!username || !password) {
        showMessage("Please enter both username and password.", 'error');
        return;
    }

    setLoading(true);
    showMessage(`Authenticating ${username}...`, 'success');

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json' 
            },
            body: JSON.stringify({ username, password })
        });

        const contentType = response.headers.get("content-type");
        const data = contentType && contentType.includes("application/json") ? await response.json() : await response.text();

        if (response.ok) {
            const jwtToken = data.token;

            if (!jwtToken) {
                showMessage("Login successful, but no JWT token received from server.", 'error');
                return;
            }
            
            localStorage.setItem('jwtToken', jwtToken);
            
            const loggedInUsername = decodeJwtForUsername(jwtToken);
            showMessage(`Welcome back, ${loggedInUsername}! Redirecting...`, 'success');
            
            setTimeout(() => { 
                window.location.href = 'homepage.html';
            }, 1000);
        } else {
            let errorMessage = 'Invalid username or password.';
            if (typeof data === 'object' && data.error) {
                errorMessage = data.error;
            } else if (typeof data === 'string') {
                errorMessage = data;
            }

            showMessage(`Login Failed: ${errorMessage}`, 'error');
        }
    } catch (error) {
        console.error("Login request failed:", error);
        showMessage("Network error. Please check your connection and try again.", 'error');
    } finally {
        setLoading(false);
    }
}