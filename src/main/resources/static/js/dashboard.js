document.addEventListener("DOMContentLoaded", () => {
    const cards = Array.from(document.querySelectorAll(".player-card"));
    const passButton = document.getElementById("pass-button");
    const likeButton = document.getElementById("like-button");
    const restartButton = document.getElementById("restart-button");
    const status = document.getElementById("match-status");
    const recentIndices = [];
    const likedIndices = new Set();
    const availableCount = () => cards.length - likedIndices.size;

    let queuedIndex;
    let currentIndex;
    let savingLike = false;

    function chooseNextIndex() {
        const available = cards.map((card, index) => index)
            .filter(index => !likedIndices.has(index));
        // Keep recent history only for players still eligible for recommendation.
        const historyLimit = Math.min(2, Math.max(0, available.length - 1));
        const recentAvailable = recentIndices.filter(index => !likedIndices.has(index));
        const excludedIndices = historyLimit > 0 ? recentAvailable.slice(-historyLimit) : [];
        const candidates = available.filter(index => !excludedIndices.includes(index));
        return candidates[Math.floor(Math.random() * candidates.length)];
    }

    function showRandomCard() {
        currentIndex = queuedIndex ?? chooseNextIndex();

        if (currentIndex !== undefined) {
            recentIndices.push(currentIndex);
            if (recentIndices.length > 2) recentIndices.shift();
        }

        // Choose once so the preview becomes the next main card on Pass.
        queuedIndex = availableCount() > 1 ? chooseNextIndex() : undefined;
        cards.forEach((card, index) => {
            const isCurrent = index === currentIndex;
            const isPreview = index === queuedIndex;
            card.hidden = !isCurrent && !isPreview;
            card.classList.toggle("is-preview", isPreview);
            card.inert = !isCurrent;
            card.setAttribute("aria-hidden", String(!isCurrent));
        });

        passButton.disabled = availableCount() < 2;
        likeButton.disabled = availableCount() === 0;
        restartButton.hidden = true;
        status.textContent = availableCount() === 0
            ? "No more players to recommend right now."
            : "";
    }

    // Keep initials visible if an avatar is missing or fails to load.
    document.querySelectorAll(".player-avatar img").forEach(image => {
        image.addEventListener("error", () => { image.hidden = true; });
        if (image.complete && image.naturalWidth === 0) image.hidden = true;
    });

    passButton.addEventListener("click", showRandomCard);

    likeButton.addEventListener("click", async () => {
        if (savingLike || currentIndex === undefined) return;
        savingLike = true;
        likeButton.disabled = true;
        passButton.disabled = true;
        likeButton.setAttribute("aria-busy", "true");
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        try {
            const response = await fetch(`/dashboard/players/${cards[currentIndex].dataset.playerId}/like`, {
                method: "POST",
                credentials: "same-origin",
                headers: token && header ? { [header]: token } : {}
            });
            if (response.status !== 204) throw new Error("Like failed");
            likedIndices.add(currentIndex);
            if (queuedIndex !== undefined && likedIndices.has(queuedIndex)) queuedIndex = undefined;
            showRandomCard();
        } catch (error) {
            status.textContent = "Could not save your Like. Please try again or sign in again.";
        } finally {
            savingLike = false;
            likeButton.disabled = availableCount() === 0;
            passButton.disabled = availableCount() < 2;
            likeButton.removeAttribute("aria-busy");
        }
    });

    showRandomCard();
});
