const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot list or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection(`users/${ALICE_UID}/cashEntries`).get());
  await assertFails(unauth.collection(`users/${ALICE_UID}/stockItems`).get());
  await assertFails(unauth.collection(`users/${ALICE_UID}/sales`).get());
  await assertFails(unauth.collection(`users/${ALICE_UID}/sellers`).get());
  await assertFails(
    unauth.doc(`users/${ALICE_UID}/settings/main`).set({
      userId: ALICE_UID,
      businessName: "Hack",
      minCashAlertCents: 1000,
      taxRatePercent: 6,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("BusinessSettings: valid create, update and cross-user denial", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    alice.doc(`users/${ALICE_UID}/settings/main`).set({
      userId: ALICE_UID,
      businessName: "Café Prumo",
      minCashAlertCents: 500000,
      taxRatePercent: 6.5,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    alice.doc(`users/${ALICE_UID}/settings/main`).update({
      businessName: "Café Prumo Ltda",
      minCashAlertCents: 800000,
      taxRatePercent: 7.0,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user access denied
  await assertFails(bob.doc(`users/${ALICE_UID}/settings/main`).get());
  await assertFails(
    bob.doc(`users/${ALICE_UID}/settings/main`).update({
      businessName: "Hacked",
      updatedAt: pastDate(1000),
    })
  );

  // Immutable createdAt cannot change
  await assertFails(
    alice.doc(`users/${ALICE_UID}/settings/main`).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
});

test("CashEntries: create, status toggle, full update, and validation bounds", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  // Empty list succeeds for owner, fails for other user
  await assertSucceeds(alice.collection(`users/${ALICE_UID}/cashEntries`).get());
  await assertFails(bob.collection(`users/${ALICE_UID}/cashEntries`).get());

  // Valid create (no `id` key)
  await assertSucceeds(
    alice.doc(`users/${ALICE_UID}/cashEntries/entry_1`).set({
      userId: ALICE_UID,
      description: "Venda balcão",
      amountCents: 450000,
      type: "INCOME",
      category: "Vendas à vista",
      account: "Caixa Loja",
      paymentMethod: "Pix",
      status: "PENDING",
      fixedExpense: false,
      dueDateMillis: Date.now(),
      createdAt: created,
      updatedAt: created,
    })
  );

  // Status toggle update succeeds
  await assertSucceeds(
    alice.doc(`users/${ALICE_UID}/cashEntries/entry_1`).update({
      status: "PAID",
      updatedAt: pastDate(1000),
    })
  );

  // Full edit update succeeds
  await assertSucceeds(
    alice.doc(`users/${ALICE_UID}/cashEntries/entry_1`).update({
      description: "Venda balcão atualizada",
      amountCents: 475000,
      type: "INCOME",
      category: "Vendas à vista",
      account: "Conta PJ",
      paymentMethod: "Cartão",
      status: "PAID",
      fixedExpense: false,
      dueDateMillis: Date.now(),
      updatedAt: pastDate(500),
    })
  );

  // Reject ghost field `id`
  await assertFails(
    alice.doc(`users/${ALICE_UID}/cashEntries/entry_2`).set({
      id: "entry_2",
      userId: ALICE_UID,
      description: "Com id extra",
      amountCents: 1000,
      type: "EXPENSE",
      category: "Fornecedores",
      account: "Conta PJ",
      paymentMethod: "Boleto",
      status: "PAID",
      fixedExpense: false,
      dueDateMillis: Date.now(),
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid enum & non-positive amount & future timestamp
  await assertFails(
    alice.doc(`users/${ALICE_UID}/cashEntries/entry_3`).set({
      userId: ALICE_UID,
      description: "Inválido",
      amountCents: 0,
      type: "INVALID",
      category: "Outros",
      account: "Caixa Loja",
      paymentMethod: "Pix",
      status: "PAID",
      fixedExpense: false,
      dueDateMillis: Date.now(),
      createdAt: new Date(Date.now() + 60000),
      updatedAt: pastDate(),
    })
  );

  // Cross-user delete denied
  await assertFails(bob.doc(`users/${ALICE_UID}/cashEntries/entry_1`).delete());
});

test("StockItems: create, stock movement update, full edit and bounds", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    alice.doc(`users/${ALICE_UID}/stockItems/sku_1`).set({
      userId: ALICE_UID,
      name: "Café 500g",
      sku: "CAF-05",
      quantity: 20,
      minStock: 10,
      leadTimeDays: 3,
      avgDailySales: 2.5,
      unitCostCents: 1800,
      unitPriceCents: 3500,
      soldUnitsCount: 15,
      lossUnitsCount: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Movement update (sale / purchase / loss)
  await assertSucceeds(
    alice.doc(`users/${ALICE_UID}/stockItems/sku_1`).update({
      quantity: 18,
      soldUnitsCount: 17,
      lossUnitsCount: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Negative quantity rejected
  await assertFails(
    alice.doc(`users/${ALICE_UID}/stockItems/sku_1`).update({
      quantity: -1,
      soldUnitsCount: 17,
      lossUnitsCount: 1,
      updatedAt: pastDate(500),
    })
  );
});

test("Sales and Sellers: create, update, list and isolation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    alice.doc(`users/${ALICE_UID}/sellers/seller_1`).set({
      userId: ALICE_UID,
      name: "Mariana Silva",
      monthlyGoalCents: 1500000,
      commissionPercent: 5.0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    alice.doc(`users/${ALICE_UID}/sales/sale_1`).set({
      userId: ALICE_UID,
      description: "Kit Café Especial",
      sellerName: "Mariana Silva",
      grossAmountCents: 32000,
      costAmountCents: 16000,
      taxPercent: 6.0,
      commissionPercent: 5.0,
      saleDateMillis: Date.now(),
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(alice.collection(`users/${ALICE_UID}/sales`).get());
  await assertSucceeds(alice.collection(`users/${ALICE_UID}/sellers`).get());
  await assertFails(bob.collection(`users/${ALICE_UID}/sales`).get());
  await assertFails(bob.collection(`users/${ALICE_UID}/sellers`).get());
});
