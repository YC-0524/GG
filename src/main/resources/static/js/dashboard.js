document.addEventListener("DOMContentLoaded", () => {
    const cards = Array.from(document.querySelectorAll(".player-card"));
    const passButton = document.getElementById("pass-button");
    const restartButton = document.getElementById("restart-button");
    const status = document.getElementById("match-status");
    const recentIndices = [];

    let queuedIndex;

    function chooseNextIndex() {
        // With fewer than three players, avoid as many repeats as possible.
        const historyLimit = Math.min(2, cards.length - 1);
        const excludedIndices = historyLimit > 0
            ? recentIndices.slice(-historyLimit)
            : [];
        const candidates = cards
            .map((card, index) => index)
            .filter(index => !excludedIndices.includes(index));
        return candidates[Math.floor(Math.random() * candidates.length)];
    }

    function showRandomCard() {
        const currentIndex = queuedIndex ?? chooseNextIndex();

        if (currentIndex !== undefined) {
            recentIndices.push(currentIndex);
            if (recentIndices.length > 2) recentIndices.shift();
        }

        // Choose once so the preview becomes the next main card on Pass.
        queuedIndex = cards.length > 1 ? chooseNextIndex() : undefined;
        cards.forEach((card, index) => {
            const isCurrent = index === currentIndex;
            const isPreview = index === queuedIndex;
            card.hidden = !isCurrent && !isPreview;
            card.classList.toggle("is-preview", isPreview);
            card.inert = !isCurrent;
            card.setAttribute("aria-hidden", String(!isCurrent));
        });

        passButton.disabled = cards.length < 2;
        restartButton.hidden = true;
        status.textContent = cards.length === 0
            ? "No other players have registered yet."
            : "";
    }

    // Keep initials visible if an avatar is missing or fails to load.
    document.querySelectorAll(".player-avatar img").forEach(image => {
        image.addEventListener("error", () => { image.hidden = true; });
        if (image.complete && image.naturalWidth === 0) image.hidden = true;
    });

    passButton.addEventListener("click", showRandomCard);

    showRandomCard();
});
