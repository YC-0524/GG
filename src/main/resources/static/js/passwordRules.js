const PASSWORD_RULES = {
    length: (value) => value.length >= 8,
    upper: (value) => /[A-Z]/.test(value),
    lower: (value) => /[a-z]/.test(value),
    number: (value) => /\d/.test(value),
    symbol: (value) => /[^A-Za-z0-9\s]/.test(value),
};

document.querySelectorAll('.password-rules').forEach(setupPasswordRules);

function setupPasswordRules(rulesList) {
    const passwordInput = document.getElementById(rulesList.dataset.passwordInput);
    const confirmInput = document.getElementById(rulesList.dataset.confirmInput);
    const matchMessage = document.getElementById(rulesList.dataset.matchMessage);

    if (!passwordInput) {
        return;
    }

    const form = passwordInput.form;
    const submitButton = form ? form.querySelector('button[type="submit"], input[type="submit"]') : null;

    let confirmTouched = false;
    let rulesPassed = false;
    let passwordsMatch = !confirmInput;

    function updateSubmitButton() {
        if (submitButton) {
            submitButton.disabled = !(rulesPassed && passwordsMatch);
        }
    }

    function checkRules() {
        const value = passwordInput.value;
        let firstMissing = null;

        rulesList.classList.toggle('is-touched', value.length > 0);

        rulesList.querySelectorAll('[data-rule]').forEach((item) => {
            const met = PASSWORD_RULES[item.dataset.rule](value);
            const status = item.querySelector('.rule-status');

            item.classList.toggle('is-met', met);
            if (status) {
                status.textContent = met ? ', met' : ', not met';
            }

            if (!met && firstMissing === null) {
                const text = item.querySelector('.rule-text').textContent.trim();
                firstMissing = text.charAt(0).toLowerCase() + text.slice(1);
            }
        });

        rulesPassed = firstMissing === null;
        rulesList.classList.toggle('is-complete', rulesPassed);
        passwordInput.setCustomValidity(firstMissing ? `Password needs ${firstMissing}.` : '');
        updateSubmitButton();
    }

    function checkMatch() {
        if (!confirmInput) {
            return;
        }

        const password = passwordInput.value;
        const confirm = confirmInput.value;
        const matches = confirm === password;

        passwordsMatch = confirm.length > 0 && matches;
        updateSubmitButton();

        confirmInput.setCustomValidity(confirm && !matches ? "Passwords don't match." : '');

        if (!matchMessage) {
            return;
        }

        const showResult = confirm.length > 0 && (confirmTouched || confirm.length >= password.length);

        matchMessage.classList.toggle('is-match', showResult && matches);
        matchMessage.classList.toggle('is-mismatch', showResult && !matches);
        matchMessage.textContent = showResult ? (matches ? 'Passwords match' : "Passwords don't match") : '';
    }

    passwordInput.addEventListener('input', () => {
        checkRules();
        checkMatch();
    });

    if (confirmInput) {
        confirmInput.addEventListener('input', checkMatch);
        confirmInput.addEventListener('blur', () => {
            confirmTouched = true;
            checkMatch();
        });
    }

    checkRules();
    checkMatch();
}
