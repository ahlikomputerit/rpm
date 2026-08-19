const state = { selectedTask: null };

const $ = (selector) => document.querySelector(selector);

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function request(url, options = {}) {
  const response = await fetch(url, { headers: { "Content-Type": "application/json" }, ...options });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.detail || "Request gagal");
  return body;
}

function statusClass(status) {
  return `status status-${String(status).replaceAll("_", "-")}`;
}

function renderTasks(tasks) {
  const target = $("#tasks");
  if (!tasks.length) {
    target.className = "stack empty";
    target.textContent = "Belum ada task.";
    return;
  }
  target.className = "stack";
  target.innerHTML = tasks.map((task) => `
    <button class="task-row" data-task-id="${escapeHtml(task.id)}">
      <div class="task-copy">
        <strong>${escapeHtml(task.instruction)}</strong>
        <span>${escapeHtml(task.created_at)} · step ${task.cursor}</span>
      </div>
      <span class="${statusClass(task.status)}">${escapeHtml(task.status)}</span>
    </button>
  `).join("");
  target.querySelectorAll("[data-task-id]").forEach((button) => {
    button.addEventListener("click", () => loadDetail(button.dataset.taskId));
  });
}

function renderApprovals(items) {
  const target = $("#approvals");
  $("#approvalCount").textContent = String(items.length);
  if (!items.length) {
    target.className = "stack empty";
    target.textContent = "Belum ada approval.";
    return;
  }
  target.className = "stack";
  target.innerHTML = items.map(({ task, approval }) => `
    <div class="approval-card">
      <div class="approval-meta"><span class="status status-waiting-approval">approval</span><span>${escapeHtml(task.instruction)}</span></div>
      <p>${escapeHtml(approval.summary)}</p>
      <div class="actions">
        <button class="primary small" data-approval="${escapeHtml(approval.id)}" data-decision="approved">Approve once</button>
        <button class="ghost small" data-approval="${escapeHtml(approval.id)}" data-decision="rejected">Reject</button>
      </div>
    </div>
  `).join("");
  target.querySelectorAll("[data-approval]").forEach((button) => {
    button.addEventListener("click", async () => {
      try {
        await request(`/api/approvals/${button.dataset.approval}/decision`, {
          method: "POST",
          body: JSON.stringify({ decision: button.dataset.decision }),
        });
        await refreshAll();
      } catch (error) {
        showMessage(error.message, true);
      }
    });
  });
}

function renderEvents(detail) {
  $("#detailPanel").classList.remove("hidden");
  $("#detailStatus").className = statusClass(detail.task.status);
  $("#detailStatus").textContent = detail.task.status;
  $("#detailInstruction").textContent = detail.task.instruction;
  $("#events").innerHTML = detail.events.map((event) => `
    <div class="event">
      <div class="event-head"><strong>${escapeHtml(event.kind)}</strong><span>${escapeHtml(event.created_at)}</span></div>
      <pre>${escapeHtml(JSON.stringify(event.payload, null, 2))}</pre>
    </div>
  `).join("") || '<div class="empty">Belum ada event.</div>';
}

async function loadDetail(taskId) {
  try {
    state.selectedTask = taskId;
    renderEvents(await request(`/api/tasks/${taskId}`));
  } catch (error) {
    showMessage(error.message, true);
  }
}

function showMessage(message, isError = false) {
  const target = $("#formMessage");
  target.className = `message ${isError ? "error" : "success"}`;
  target.textContent = message;
  window.setTimeout(() => target.classList.add("hidden"), 4500);
}

async function refreshAll() {
  try {
    const [tasks, approvals] = await Promise.all([request("/api/tasks"), request("/api/approvals/pending")]);
    renderTasks(tasks);
    renderApprovals(approvals);
    if (state.selectedTask) {
      const detail = await request(`/api/tasks/${state.selectedTask}`);
      renderEvents(detail);
    }
  } catch (error) {
    showMessage(error.message, true);
  }
}

$("#taskForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const domains = $("#domains").value.split(",").map((item) => item.trim()).filter(Boolean);
  try {
    const task = await request("/api/tasks", {
      method: "POST",
      body: JSON.stringify({
        instruction: $("#instruction").value,
        start_url: $("#startUrl").value || null,
        allowed_domains: domains,
        mode: $("#mode").value,
      }),
    });
    $("#instruction").value = "";
    showMessage(`Task dibuat: ${task.id}`);
    await refreshAll();
    await loadDetail(task.id);
  } catch (error) {
    showMessage(error.message, true);
  }
});

$("#refresh").addEventListener("click", refreshAll);
$("#stopAll").addEventListener("click", async () => {
  if (!confirm("Hentikan semua task aktif?")) return;
  try {
    const result = await request("/api/emergency-stop", { method: "POST" });
    showMessage(`${result.cancelled} task dihentikan.`);
    await refreshAll();
  } catch (error) {
    showMessage(error.message, true);
  }
});

refreshAll();
window.setInterval(refreshAll, 2500);
