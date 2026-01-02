// announcements.js

/** Function to fetch and render announcements */
window.fetchAnnouncements = async function(groupId) {
    const token = localStorage.getItem('jwtToken');
    if (!token || !window.apiUrlBase) return;

    const listEl = document.getElementById('announcements-list');
    const noAnnouncementsEl = document.getElementById('no-announcements');

    if (listEl && noAnnouncementsEl) {
        listEl.innerHTML = '';
        noAnnouncementsEl.textContent = "Fetching announcements...";
        noAnnouncementsEl.classList.remove('hidden');
        listEl.appendChild(noAnnouncementsEl);
    }

    try {
        const response = await fetch(`${window.apiUrlBase}${groupId}/announcements`, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch announcements. Status: ${response.status}`);
        }

        const announcements = await response.json();
        renderAnnouncements(announcements);

    } catch (error) {
        window.handleError(`Error loading announcements: ${error.message}`);
    }
}

/** Function to render announcements */
function renderAnnouncements(announcements) {
    const list = document.getElementById('announcements-list');
    const noAnnouncementsEl = document.getElementById('no-announcements');
    if (!list || !noAnnouncementsEl) return;

    list.innerHTML = '';
    
    if (!announcements || announcements.length === 0) {
        noAnnouncementsEl.textContent = "No recent announcements.";
        noAnnouncementsEl.classList.remove('hidden');
        list.appendChild(noAnnouncementsEl);
        return;
    }
    noAnnouncementsEl.classList.add('hidden');

    const sortedAnnouncements = announcements.sort((a, b) => 
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );

    sortedAnnouncements.forEach(announcement => {
        const item = document.createElement('div');
        item.className = 'bg-white p-5 rounded-xl shadow-md border-l-4 border-orange-500 space-y-2';
        
        const titleHtml = announcement.title 
            ? `<h3 class="text-xl font-bold text-orange-700">${announcement.title}</h3>` 
            : '';
            
        const formattedDate = new Date(announcement.createdAt).toLocaleString();
            
        item.innerHTML = `
            ${titleHtml}
            <p class="text-gray-800 whitespace-pre-wrap">${announcement.description}</p>
            <div class="text-sm text-gray-500 pt-2 border-t border-gray-100">
                Posted by: <span class="font-medium">${announcement.createdByUsername}</span> 
                on ${formattedDate}
            </div>
        `;
        list.appendChild(item);
    });
}

/** Handles the submission of a new announcement. */
async function handleAddAnnouncement(event) {
    event.preventDefault();
    let token = localStorage.getItem('jwtToken');
    
    if (!window.currentGroupId || !token) {
        window.handleError("Error: Group or authentication token missing. Please re-login."); 
        return;
    }

    const form = event.target;
    const announcementData = {
        title: form.announcementTitle.value.trim(),
        description: form.announcementDescription.value.trim()
    };
    
    if (announcementData.description.length < 5) {
        window.alertUserMessage("Announcement message must be at least 5 characters long.", 'warning');
        return;
    }

    const submitBtn = document.getElementById('submit-announcement-btn');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Posting...';
    }

    try {
        const response = await fetch(`${window.apiUrlBase}${window.currentGroupId}/announcements`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}` 
            },
            body: JSON.stringify(announcementData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            let errorMessage = `HTTP Error ${response.status}: ${response.statusText}.`;
            try {
                const errorJson = JSON.parse(errorText);
                errorMessage = `Server Error: ${errorJson.message || 'Unknown error'}`;
            } catch (e) {
                errorMessage += ` Server message snippet: ${errorText.substring(0, 100)}...`;
            }
            throw new Error(errorMessage);
        }
        
        window.closeModal('announcement-modal');
        window.alertUserMessage("Announcement posted successfully. Refreshing list...");
        
        // Re-fetch and re-render the list
        await window.fetchAnnouncements(window.currentGroupId); 
        window.switchTab('announcements'); 

    } catch (error) {
        window.handleError(`Failed to post announcement: ${error.message}`);
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Post Announcement';
        }
    }
}

// Attach event listener when the module loads
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('add-announcement-form')?.addEventListener('submit', handleAddAnnouncement);
});