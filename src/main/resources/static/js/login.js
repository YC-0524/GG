const passwordInput = document.getElementById('password');
const passwordToggle = document.querySelector('.password-toggle');

passwordToggle.addEventListener('click', () => {
    const showPassword = passwordInput.type === 'password';

    passwordInput.type = showPassword ? 'text' : 'password';
    passwordToggle.setAttribute('aria-label', showPassword ? 'Hide password' : 'Show password');
    passwordToggle.setAttribute('aria-pressed', String(showPassword));
});

const error = new URLSearchParams(window.location.search).get('error');

if (error) {
    const errorMessage = document.getElementById('login-error');

    errorMessage.textContent = error;
    errorMessage.hidden = false;
}
