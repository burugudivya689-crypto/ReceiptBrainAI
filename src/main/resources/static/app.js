const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
let receiptModal;
const authHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` });

async function api(url, options = {}) {
    const response = await fetch(url, options);
    if (response.status === 401) {
        localStorage.removeItem('token');
        showLogin();
        document.getElementById('workspace').classList.add('d-none');
        document.getElementById('authSection').classList.remove('d-none');
        document.getElementById('loginButton').classList.remove('d-none');
        document.getElementById('registerButton').classList.remove('d-none');
        document.getElementById('logoutButton').classList.add('d-none');
        throw new Error('Your session expired. Please log in again.');
    }
    if (!response.ok) { const error = await response.json().catch(() => null); throw new Error(error?.message || 'Something went wrong.'); }
    return response.json();
}

async function registerUser() {
    try { const data = await api('/api/auth/register', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(formCredentials(true))}); localStorage.setItem('token',data.token); enterWorkspace(); }
    catch (error) { showAuthMessage(error.message, 'danger'); }
}
async function loginUser() {
    try { const data = await api('/api/auth/login', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(formCredentials(false))}); localStorage.setItem('token',data.token); enterWorkspace(); }
    catch (error) { showAuthMessage('Login failed. Check your email and password.', 'danger'); }
}
function formCredentials(includeName) { return { ...(includeName ? {fullName:document.getElementById('fullName').value}:{}), email:document.getElementById('email').value, password:document.getElementById('password').value }; }
function showAuthMessage(message, type) { document.getElementById('authMessage').innerHTML = `<div class="alert alert-${type} py-2 mt-3 mb-0">${message}</div>`; }
function showLogin() { document.getElementById('authBox').innerHTML = `<h2 class="h4">Welcome back</h2><p class="text-secondary small">Log in to access your receipt library.</p><form onsubmit="event.preventDefault(); loginUser();"><input id="email" class="form-control mb-2" type="email" placeholder="Email" required><input id="password" class="form-control mb-2" type="password" placeholder="Password" required><button class="btn btn-primary">Login</button></form><div id="authMessage"></div>`; }
function showRegister() { document.getElementById('authBox').innerHTML = `<h2 class="h4">Create your receipt vault</h2><p class="text-secondary small">Free, local demo account — ready in seconds.</p><form onsubmit="event.preventDefault(); registerUser();"><input id="fullName" class="form-control mb-2" placeholder="Full name" required><input id="email" class="form-control mb-2" type="email" placeholder="Email" required><input id="password" class="form-control mb-2" type="password" minlength="6" placeholder="Password (at least 6 characters)" required><button class="btn btn-primary">Create account</button></form><div id="authMessage"></div>`; }
function enterWorkspace() { document.getElementById('authSection').classList.add('d-none'); document.getElementById('workspace').classList.remove('d-none'); document.getElementById('loginButton').classList.add('d-none'); document.getElementById('registerButton').classList.add('d-none'); document.getElementById('logoutButton').classList.remove('d-none'); loadDashboard(); }
function logout() { localStorage.removeItem('token'); location.reload(); }

async function uploadReceipt() {
    const file = document.getElementById('receiptFile').files[0]; if (!file) return setUploadStatus('Choose an image or PDF first.', 'danger');
    setUploadStatus('Uploading and preparing receipt…', 'secondary'); const body = new FormData(); body.append('file', file);
    try { const receipt = await api('/api/receipts/upload', {method:'POST',headers:authHeaders(),body}); setUploadStatus('Saved. Please verify the suggested details.', 'success'); openReceipt(receipt); loadDashboard(); }
    catch (error) { setUploadStatus(error.message, 'danger'); }
}
function setUploadStatus(message, type) { document.getElementById('uploadStatus').innerHTML = `<span class="text-${type}">${message}</span>`; }

async function loadDashboard() {
    if (!localStorage.getItem('token')) return;
    const payload = {merchant:document.getElementById('searchMerchant').value,category:document.getElementById('searchCategory').value,query:document.getElementById('searchQuery').value};
    try { const [summary, receipts, alerts] = await Promise.all([api('/api/analytics/summary',{headers:authHeaders()}),api('/api/receipts/search',{method:'POST',headers:{'Content-Type':'application/json',...authHeaders()},body:JSON.stringify(payload)}),api('/api/receipts/warranties/alerts',{headers:authHeaders()})]); renderSummary(summary); renderReceipts(receipts); renderAlerts(alerts); }
    catch (error) { console.error(error); }
}
function renderSummary(s) { document.getElementById('summaryCards').innerHTML = [['Receipts',s.totalReceipts],['Total spending',currency.format(s.totalSpending || 0)],['Average receipt',currency.format(s.averageReceipt || 0)],['Highest expense',currency.format(s.highestExpense || 0)]].map(([label,value]) => `<div class="col-6 col-lg-3"><div class="metric-card"><span>${label}</span><strong>${value}</strong></div></div>`).join(''); }
function renderReceipts(receipts) { const list = document.getElementById('receiptList'); list.innerHTML = receipts.length ? receipts.map(r => `<button class="receipt-row" onclick="openReceiptById(${r.id})"><span class="receipt-icon">⌾</span><span class="flex-grow-1 text-start"><strong>${escapeHtml(r.merchant || 'New receipt')}</strong><small>${r.purchaseDate || 'Date not set'} · ${r.category || 'Others'}${r.warrantyExpiryDate ? ` · Warranty until ${r.warrantyExpiryDate}` : ''}</small></span><strong>${currency.format(r.amount || 0)}</strong><span class="ms-2 text-secondary">›</span></button>`).join('') : '<div class="empty-state">No receipts match these filters. Upload your first receipt above.</div>'; }
function renderAlerts(alerts) { document.getElementById('alertCount').textContent = alerts.length; document.getElementById('warrantyAlerts').innerHTML = alerts.length ? alerts.map(a => `<div class="alert-row"><span class="status-dot ${a.daysRemaining < 0 ? 'expired' : ''}"></span><span><strong>${escapeHtml(a.merchant)}</strong><br>${a.status} · ${a.expiresOn}</span></div>`).join('') : 'No warranties expire in the next 60 days.'; }
async function openReceiptById(id) { try { openReceipt(await api(`/api/receipts/${id}`,{headers:authHeaders()})); } catch (e) { alert(e.message); } }
function openReceipt(r) { document.getElementById('editId').value=r.id; document.getElementById('editMerchant').value=r.merchant || ''; document.getElementById('editDate').value=r.purchaseDate || new Date().toISOString().slice(0,10); document.getElementById('editAmount').value=r.amount || 0; document.getElementById('editCategory').value=r.category || 'Others'; document.getElementById('editWarranty').value=r.warrantyMonths || 0; document.getElementById('editPayment').value=r.paymentMethod || ''; receiptModal.show(); }
async function saveReceipt() { const payload={merchant:document.getElementById('editMerchant').value,purchaseDate:document.getElementById('editDate').value,amount:Number(document.getElementById('editAmount').value),currency:'INR',category:document.getElementById('editCategory').value,paymentMethod:document.getElementById('editPayment').value,warrantyMonths:Number(document.getElementById('editWarranty').value || 0)}; try { await api(`/api/receipts/${document.getElementById('editId').value}`,{method:'PUT',headers:{'Content-Type':'application/json',...authHeaders()},body:JSON.stringify(payload)}); receiptModal.hide(); loadDashboard(); } catch (e) { alert(e.message); } }
function clearSearch() { ['searchMerchant','searchCategory','searchQuery'].forEach(id => document.getElementById(id).value=''); loadDashboard(); }
function escapeHtml(value) { const span=document.createElement('span'); span.textContent=value; return span.innerHTML; }
document.addEventListener('DOMContentLoaded', () => { receiptModal = new bootstrap.Modal(document.getElementById('receiptModal')); localStorage.getItem('token') ? enterWorkspace() : showLogin(); });
