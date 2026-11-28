const MAX_HASHTAGS = 5;

// shows character limit
document.querySelectorAll("[data-count-for]").forEach((counter) => {
    const field = document.getElementById(counter.dataset.countFor);
    if (!field) return;
    const update = () => {
        counter.textContent = `${field.value.length}/${field.maxLength}`;
    };
    field.addEventListener("input", update);
    update();
});

const hashtagInput = document.getElementById("hashtags");

if (hashtagInput) {
    const hashtagCount = document.getElementById("hashtag-count");
    const suggestions = document.querySelectorAll(".sos-tag-suggestion");

    // hashtags
    function currentTags() {
        const tags = hashtagInput.value
            .split(/[\s,]+/)
            .map((tag) => tag.replace(/^#+/, "").toLowerCase())
            .filter((tag) => tag !== "");
        return [...new Set(tags)];
    }

    function updateHashtags() {
        const tags = currentTags();
        hashtagCount.textContent = `${tags.length}/${MAX_HASHTAGS}`;
        hashtagInput.setCustomValidity(
            tags.length > MAX_HASHTAGS ? `Use up to ${MAX_HASHTAGS} hashtags.` : "");

        suggestions.forEach((button) => {
            const used = tags.includes(button.dataset.tag);
            button.setAttribute("aria-pressed", String(used));
            // click again to remove tag
            button.disabled = !used && tags.length >= MAX_HASHTAGS;
        });
    }

    // clicking onto a suggestion adds it, or removes it if it is already there
    suggestions.forEach((button) => {
        button.addEventListener("click", () => {
            const tag = button.dataset.tag;
            const tags = currentTags();
            const next = tags.includes(tag) ? tags.filter((t) => t !== tag) : [...tags, tag];
            hashtagInput.value = next.map((t) => "#" + t).join(" ");
            updateHashtags();
        });
    });

    hashtagInput.addEventListener("input", updateHashtags);
    updateHashtags();
}