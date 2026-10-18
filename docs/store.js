/* Penyimpanan lokal + sinkron Firestore (REST). Format data sama persis dengan aplikasi Android. */
(function (root) {
  'use strict';
  const CFG = root.SAPPA_CONFIG;
  const S = { recs: {}, data: null, onChange: function () {}, status: 'Belum diatur', lastSync: 0, busy: false };
  const LS = {
    get(k, d) { try { const v = localStorage.getItem(k); return v === null ? d : v; } catch (e) { return d; } },
    set(k, v) { try { localStorage.setItem(k, v); } catch (e) { /* abaikan */ } },
  };
  S.LS = LS;

  S.load = function () {
    try { S.recs = JSON.parse(LS.get('ds_recs', '{}')) || {}; } catch (e) { S.recs = {}; }
    S.lastSync = +LS.get('ds_lastsync', '0');
    S.data = root.Core.buildData(S.recs);
  };
  function save() { LS.set('ds_recs', JSON.stringify(S.recs)); }
  function rebuild() { S.data = root.Core.buildData(S.recs); }

  S.put = function (o) {
    o.u = Date.now(); o._d = true;
    S.recs[o.id] = o; save(); rebuild(); S.onChange(true); S.kick();
  };
  S.project = () => LS.get('ds_project', '') || CFG.FIREBASE_PROJECT_ID;
  S.apiKey = () => LS.get('ds_key', '') || CFG.FIREBASE_API_KEY;

  let token = null, tokenExp = 0, timer = null;
  async function ensureToken(key) {
    const now = Date.now();
    if (token && now < tokenExp - 300000) return token;
    const r = await fetch('https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=' + encodeURIComponent(key), {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ returnSecureToken: true }),
    });
    if (!r.ok) throw new Error('Login anonim gagal (' + r.status + '). Aktifkan Anonymous di Firebase Auth.');
    const j = await r.json();
    token = j.idToken; tokenExp = now + (+j.expiresIn || 3600) * 1000;
    return token;
  }

  S.syncOnce = async function () {
    const project = S.project(), key = S.apiKey();
    if (!project || !key) { S.status = 'Belum diatur (mode lokal)'; S.onChange(false); return; }
    if (S.busy) return;
    S.busy = true;
    try {
      const tk = await ensureToken(key);
      const base = 'https://firestore.googleapis.com/v1/projects/' + project + '/databases/(default)/documents';
      const H = { 'Content-Type': 'application/json', Authorization: 'Bearer ' + tk };
      // kirim
      const dirty = Object.keys(S.recs).filter((id) => S.recs[id]._d);
      for (const id of dirty) {
        const o = JSON.parse(JSON.stringify(S.recs[id])); const u = o.u; delete o._d;
        const body = { fields: { d: { stringValue: JSON.stringify(o) }, u: { integerValue: String(u) } } };
        const r = await fetch(base + '/shops/dapursappa/records/' + encodeURIComponent(id) + '?updateMask.fieldPaths=d&updateMask.fieldPaths=u', { method: 'PATCH', headers: H, body: JSON.stringify(body) });
        if (!r.ok) throw new Error('Kirim gagal (' + r.status + ')');
        if (S.recs[id] && S.recs[id].u === u) { delete S.recs[id]._d; }
      }
      save();
      // ambil
      const lastPull = +LS.get('ds_lastpull', '0');
      const since = Math.max(0, lastPull - 120000);
      const q = { structuredQuery: { from: [{ collectionId: 'records' }], where: { fieldFilter: { field: { fieldPath: 'u' }, op: 'GREATER_THAN', value: { integerValue: String(since) } } } } };
      const r2 = await fetch(base + '/shops/dapursappa:runQuery', { method: 'POST', headers: H, body: JSON.stringify(q) });
      if (!r2.ok) throw new Error('Ambil gagal (' + r2.status + ')');
      const arr = await r2.json();
      let maxU = lastPull, changed = false;
      arr.forEach((it) => {
        if (!it.document || !it.document.fields || !it.document.fields.d) return;
        try {
          const rec = JSON.parse(it.document.fields.d.stringValue);
          const uu = it.document.fields.u ? +it.document.fields.u.integerValue : (rec.u || 0);
          rec.u = uu; delete rec._d;
          if (uu > maxU) maxU = uu;
          const local = S.recs[rec.id];
          if (!local || rec.u > (local.u || 0)) { S.recs[rec.id] = rec; changed = true; }
        } catch (e) { /* lewati */ }
      });
      LS.set('ds_lastpull', String(maxU));
      S.lastSync = Date.now(); LS.set('ds_lastsync', String(S.lastSync));
      S.status = 'Online';
      if (changed) { save(); rebuild(); S.onChange(true); } else { S.onChange(false); }
    } catch (e) {
      S.status = 'Offline · ' + String(e.message || e).substring(0, 70);
      S.onChange(false);
    } finally { S.busy = false; }
  };
  S.kick = function () { setTimeout(S.syncOnce, 300); };
  S.start = function () { S.syncOnce(); if (timer) clearInterval(timer); timer = setInterval(S.syncOnce, 12000); };

  root.Store = S;
})(window);
