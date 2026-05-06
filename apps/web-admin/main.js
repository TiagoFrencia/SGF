const apiBase = "http://localhost:8080";
const state = {
  token: null,
  user: null,
  lastProductId: null
};

const logOutput = document.getElementById("log-output");
const stockOutput = document.getElementById("stock-output");
const auditOutput = document.getElementById("audit-output");

function log(message, payload) {
  const timestamp = new Date().toLocaleTimeString();
  const serialized = payload ? `\n${JSON.stringify(payload, null, 2)}` : "";
  logOutput.textContent = `[${timestamp}] ${message}${serialized}\n\n${logOutput.textContent}`;
}

function setAuthStatus() {
  document.getElementById("api-status").textContent = state.token ? "Autenticado" : "No autenticado";
  document.getElementById("current-user").textContent = state.user ?? "-";
}

async function request(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers ?? {})
  };
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(data?.message ?? "Unexpected API error");
  }
  return data;
}

document.getElementById("login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    const data = await request("/auth/login", {
      method: "POST",
      body: JSON.stringify({
        username: form.get("username"),
        password: form.get("password")
      })
    });
    state.token = data.accessToken;
    state.user = data.username;
    setAuthStatus();
    log("Login correcto", data);
  } catch (error) {
    log(`Login fallido: ${error.message}`);
  }
});

document.getElementById("product-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    const data = await request("/products", {
      method: "POST",
      body: JSON.stringify({
        gtin: form.get("gtin"),
        sku: form.get("sku"),
        commercialName: form.get("commercialName"),
        brand: form.get("brand"),
        activeIngredient: form.get("activeIngredient"),
        prescriptionRequired: form.get("prescriptionRequired") === "on",
        presentationDescription: form.get("presentationDescription"),
        concentration: form.get("concentration"),
        form: form.get("form"),
        unitsPerPackage: Number(form.get("unitsPerPackage"))
      })
    });
    state.lastProductId = data.id;
    document.querySelectorAll('#receipt-form input[name="productId"], #sale-form input[name="productId"]').forEach((input) => {
      input.value = data.id;
    });
    log("Producto creado", data);
  } catch (error) {
    log(`Error creando producto: ${error.message}`);
  }
});

document.getElementById("receipt-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    const data = await request("/inventory/receipts", {
      method: "POST",
      body: JSON.stringify({
        productId: form.get("productId"),
        lotNumber: form.get("lotNumber"),
        expiresAt: form.get("expiresAt"),
        quantity: Number(form.get("quantity")),
        unitCost: Number(form.get("unitCost"))
      })
    });
    log("Ingreso de stock registrado", data);
    await refreshStock();
  } catch (error) {
    log(`Error ingresando stock: ${error.message}`);
  }
});

document.getElementById("sale-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    const data = await request("/sales", {
      method: "POST",
      body: JSON.stringify({
        idempotencyKey: form.get("idempotencyKey"),
        items: [{
          productId: form.get("productId"),
          quantity: Number(form.get("quantity")),
          unitPrice: Number(form.get("unitPrice"))
        }]
      })
    });
    log("Venta procesada", data);
    await Promise.all([refreshStock(), refreshAudit()]);
  } catch (error) {
    log(`Error procesando venta: ${error.message}`);
  }
});

async function refreshStock() {
  try {
    const data = await request("/inventory/stock");
    stockOutput.textContent = JSON.stringify(data, null, 2);
  } catch (error) {
    log(`Error consultando stock: ${error.message}`);
  }
}

async function refreshAudit() {
  try {
    const data = await request("/audit/events");
    auditOutput.textContent = JSON.stringify(data, null, 2);
  } catch (error) {
    log(`Error consultando auditoria: ${error.message}`);
  }
}

document.getElementById("refresh-stock").addEventListener("click", refreshStock);
document.getElementById("refresh-audit").addEventListener("click", refreshAudit);

const nextYear = new Date();
nextYear.setFullYear(nextYear.getFullYear() + 1);
document.querySelector('#receipt-form input[name="expiresAt"]').value = nextYear.toISOString().slice(0, 10);
setAuthStatus();
