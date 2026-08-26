// On This Day — infinite scroll + share.
(function () {
    "use strict";

    initInfiniteScroll();
    initShareButtons();

    function initInfiniteScroll() {
        const grid = document.getElementById("grid");
        const sentinel = document.getElementById("sentinel");
        if (!grid || !sentinel) return;

        const state = {
            month: grid.dataset.month,
            day: grid.dataset.day,
            tag: grid.dataset.tag || null,
            size: parseInt(grid.dataset.size || "24", 10),
            page: parseInt(grid.dataset.nextpage || "1", 10),
            hasNext: grid.dataset.hasnext === "true",
            loading: false
        };

        const loader = document.createElement("div");
        loader.className = "loader";
        loader.style.display = "none";
        loader.textContent = "Loading more…";
        sentinel.parentNode.insertBefore(loader, sentinel);

        const observer = new IntersectionObserver(function (entries) {
            if (entries.some(function (e) { return e.isIntersecting; })) {
                loadMore();
            }
        }, { rootMargin: "600px 0px" });
        observer.observe(sentinel);

        function loadMore() {
            if (state.loading || !state.hasNext) return;
            state.loading = true;
            loader.style.display = "block";

            const params = new URLSearchParams({ page: state.page, size: state.size });
            if (state.tag) params.set("tag", state.tag);
            const url = "/api/digest/" + state.month + "/" + state.day + "?" + params.toString();

            fetch(url, { headers: { "Accept": "application/json" } })
                .then(function (r) {
                    if (!r.ok) throw new Error("HTTP " + r.status);
                    return r.json();
                })
                .then(function (data) {
                    (data.content || []).forEach(function (e) { grid.appendChild(renderTile(e)); });
                    state.page += 1;
                    state.hasNext = !!data.hasNext;
                    state.loading = false;
                    loader.style.display = "none";
                    if (!state.hasNext) observer.unobserve(sentinel);
                })
                .catch(function (err) {
                    console.error("Failed to load more events:", err);
                    state.loading = false;
                    loader.textContent = "Couldn't load more.";
                });
        }
    }

    // Mirror of the server-rendered tile markup in home.html.
    function renderTile(e) {
        const a = document.createElement("a");
        a.className = "tile tile--" + String(e.feedType || "events").toLowerCase();
        a.href = "/event/" + e.id;

        const media = document.createElement("div");
        media.className = "tile__media";
        if (e.thumbnailUrl) {
            const img = document.createElement("img");
            img.src = e.thumbnailUrl;
            img.alt = e.title || "";
            img.loading = "lazy";
            media.appendChild(img);
        }
        if (e.year !== null && e.year !== undefined) {
            const badge = document.createElement("span");
            badge.className = "tile__badge";
            badge.textContent = e.year;
            media.appendChild(badge);
        }
        const feed = document.createElement("span");
        feed.className = "tile__feed";
        feed.textContent = String(e.feedType || "").toLowerCase();
        media.appendChild(feed);

        const body = document.createElement("div");
        body.className = "tile__body";
        const text = document.createElement("p");
        text.className = "tile__text";
        text.textContent = e.text || "";
        body.appendChild(text);

        const tags = document.createElement("div");
        tags.className = "tile__tags";
        (e.tags || []).forEach(function (t) {
            const chip = document.createElement("span");
            chip.className = "minichip";
            chip.textContent = t;
            tags.appendChild(chip);
        });
        body.appendChild(tags);

        a.appendChild(media);
        a.appendChild(body);
        return a;
    }

    function initShareButtons() {
        document.querySelectorAll("[data-share]").forEach(function (btn) {
            btn.addEventListener("click", function () {
                const shareData = { title: btn.dataset.title || "On This Day", url: window.location.href };
                if (navigator.share) {
                    navigator.share(shareData).catch(function () { /* user cancelled */ });
                } else if (navigator.clipboard) {
                    navigator.clipboard.writeText(window.location.href).then(function () {
                        toast("Link copied to clipboard");
                    });
                } else {
                    toast(window.location.href);
                }
            });
        });
    }

    function toast(message) {
        let el = document.querySelector(".toast");
        if (!el) {
            el = document.createElement("div");
            el.className = "toast";
            document.body.appendChild(el);
        }
        el.textContent = message;
        el.classList.add("toast--show");
        setTimeout(function () { el.classList.remove("toast--show"); }, 2200);
    }
})();
