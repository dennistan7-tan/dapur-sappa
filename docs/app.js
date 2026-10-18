/* Dapur Sappa · versi web. Memakai data Firebase yang sama dengan aplikasi Android. */
(function () {
  'use strict';
  const C = window.Core, Store = window.Store, CFG = window.SAPPA_CONFIG, LS = Store.LS;
  const LOGO = window.LOGO || 'logo.png';
  const $ = (s) => document.querySelector(s);
  const esc = (s) => String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const D = () => Store.data;
  const digits = (s) => String(s || '').replace(/\D/g, '').substring(0, 12);
  const today = () => C.today();

  const st = {
    tab: null, belDate: null, orderDate: null, weekStart: null, repTab: 2, repMonth: null, repWeek: null,
    route: null, f: {}, m: null, modal: null, rf: 'Semua',
  };

  // ---------- user ----------
  function getMe() {
    if (LS.get('ds_dev', '') === '1') return { idx: -1, isDev: true, name: 'Developer', role: 'Developer', label: 'Developer', canAdmin: true };
    const i = +LS.get('ds_user', '-1');
    const u = D().users[i];
    if (!u) return null;
    return { idx: i, isDev: false, name: u.name, role: u.role, label: u.role, canAdmin: u.role === 'Admin' };
  }
  const canEdit = (me, date) => me.canAdmin || date === today();
  const userName = (idx) => (idx < 0 ? 'Developer' : (D().users[idx] ? D().users[idx].name : 'User ' + (idx + 1)));
  const devUser = () => LS.get('ds_devuser', '') || CFG.DEV_DEFAULT_USER;
  const devHash = () => LS.get('ds_devhash', '') || CFG.DEV_DEFAULT_HASH;

  // ---------- repo (semua perubahan tercatat di riwayat) ----------
  function log(me, cat, title, detail) {
    Store.put({ t: 'a', id: C.uuid(), ts: Date.now(), who: me.name + ' · ' + me.label, cat, title, detail });
  }
  const kindLabel = (k) => C.KINDS[k] || k;
  const purchaseLine = (p) => (C.num(p.qty) + ' ' + p.unit).trim() + ' · ' + C.rp(p.price) + ' · ' + kindLabel(p.kind);
  function savePurchase(p, old, me) {
    Store.put(p);
    if (!old) return log(me, 'Belanja', 'Tambah belanja: ' + p.name, purchaseLine(p));
    const ch = [];
    if (old.name !== p.name) ch.push('Nama ' + old.name + ' → ' + p.name);
    if (old.price !== p.price) ch.push('Harga ' + C.rp(old.price) + ' → ' + C.rp(p.price));
    if (old.qty !== p.qty || old.unit !== p.unit) ch.push('Jumlah ' + C.num(old.qty) + ' ' + old.unit + ' → ' + C.num(p.qty) + ' ' + p.unit);
    if (old.kind !== p.kind) ch.push('Jenis ' + kindLabel(old.kind) + ' → ' + kindLabel(p.kind));
    if (old.useDate !== p.useDate) ch.push('Untuk tanggal ' + old.useDate + ' → ' + p.useDate);
    if (ch.length) log(me, 'Belanja', 'Edit belanja: ' + p.name, ch.join('\n'));
  }
  function deletePurchase(p, me) {
    Store.put(Object.assign({}, p, { del: true }));
    log(me, 'Belanja', 'Hapus belanja: ' + p.name, purchaseLine(p) + ' · Dihapus');
  }
  const orderLine = (o) => (o.only ? 'Lauk saja' : o.pax + ' pax') + ' · ' + C.rp(C.orderTotal(o)) + ' · ' + (o.paid ? 'Lunas' : 'Belum bayar');
  function saveOrder(o, old, me) {
    Store.put(o);
    if (!old) return log(me, 'Order', 'Tambah order ' + o.customer, orderLine(o));
    const ch = [];
    if (old.customer !== o.customer) ch.push('Nama ' + old.customer + ' → ' + o.customer);
    if (old.location !== o.location) ch.push('Lokasi ' + old.location + ' → ' + o.location);
    if (old.pax !== o.pax) ch.push('Pax ' + old.pax + ' → ' + o.pax);
    if (old.price !== o.price) ch.push('Harga porsi ' + C.rp(old.price) + ' → ' + C.rp(o.price));
    if (C.extrasTotal(old) !== C.extrasTotal(o)) ch.push('Tambahan ' + C.rp(C.extrasTotal(old)) + ' → ' + C.rp(C.extrasTotal(o)));
    if (old.paid !== o.paid) ch.push(o.paid ? 'Belum bayar → Lunas' : 'Lunas → Belum bayar');
    if (old.only !== o.only) ch.push(o.only ? 'Jadi lauk saja' : 'Jadi porsi');
    if (old.date !== o.date) ch.push('Tanggal ' + old.date + ' → ' + o.date);
    if (old.remarks !== o.remarks) ch.push('Remarks diubah');
    if (ch.length) log(me, 'Order', 'Edit order ' + o.customer, ch.join('\n'));
  }
  function deleteOrder(o, me) {
    Store.put(Object.assign({}, o, { del: true }));
    log(me, 'Order', 'Hapus order ' + o.customer, orderLine(o) + ' · Dihapus');
  }
  function saveExpense(e, me) {
    Store.put(e);
    log(me, 'Pengeluaran', 'Tambah pengeluaran: ' + e.name, C.rp(e.amount) + ' · ' + (e.monthly ? 'Profit Bulanan' : 'Profit Harian'));
  }
  function deleteExpense(e, me) {
    Store.put(Object.assign({}, e, { del: true }));
    log(me, 'Pengeluaran', 'Hapus pengeluaran: ' + e.name, C.rp(e.amount) + ' · Dihapus');
  }
  function saveMenu(m, old, me) {
    Store.put({ t: 'm', id: 'menu-' + m.week, week: m.week, days: m.days, libur: m.libur, price: m.price });
    const base = old || { days: ['', '', '', '', '', '', ''], libur: [false, false, false, false, false, false, true], price: 20000 };
    const ch = [];
    for (let i = 0; i < 7; i++) {
      const dn = C.DAYS[i];
      if (base.days[i] !== m.days[i]) ch.push(dn + ': ' + (base.days[i].replace(/\n/g, ' / ') || 'Belum diisi') + ' → ' + (m.days[i].replace(/\n/g, ' / ') || 'Belum diisi'));
      if (base.libur[i] !== m.libur[i]) ch.push(dn + ': ' + (m.libur[i] ? 'Libur' : 'Buka'));
    }
    if (base.price !== m.price) ch.push('Harga porsi ' + C.rp(base.price) + ' → ' + C.rp(m.price));
    if (ch.length) log(me, 'Menu', 'Ubah menu Week ' + C.weekNo(m.week), ch.join('\n'));
  }
  function saveUsers(users, me) {
    Store.put({ t: 'c', id: 'cfg', users });
    log(me, 'User', 'Ubah daftar user', users.map((u) => u.name + ' (' + u.role + ')').join(', '));
  }

  // ---------- komponen html ----------
  const tile = (l, v, hi) => `<div class="tile${hi ? ' hi' : ''}"><small>${esc(l)}</small><b>${esc(v)}</b></div>`;
  const banner = (s, b) => `<div class="banner"><small>${esc(s)}</small><b>${esc(b)}</b></div>`;
  const chip = (label, on, a, v) => `<button type="button" class="chip${on ? ' on' : ''}" data-a="${a}" data-v="${esc(v === undefined ? '' : v)}">${esc(label)}</button>`;
  const tag = (t, kind) => `<span class="tag ${kind || ''}">${esc(t)}</span>`;
  const kindTag = (k) => tag(kindLabel(k), k === 'BAHAN' ? 'ok' : k === 'KEMASAN' ? 'ac' : 'wr');
  const nav = (title, sub, prev, next) => `<div class="wk"><button class="arr" data-a="${prev}">‹</button><div class="ttl"><h2>${esc(title)}</h2><p>${esc(sub)}</p></div><button class="arr" data-a="${next}">›</button></div>`;
  const field = (label, bind, val, extra) => `<label class="fld"><span>${esc(label)}</span><input class="in" data-bind="${bind}" value="${esc(val)}" ${extra || ''}></label>`;
  const menuBanner = (date) => {
    const mn = C.menuName(D(), date), lib = C.isLibur(D(), date);
    return C.dayLong(date) + ' · ' + (mn || (lib ? 'Libur' : 'Menu belum diisi'));
  };

  // ---------- auth ----------
  function renderAuth(stage) {
    const users = D().users;
    let h = '<div class="auth"><img src="' + LOGO + '" alt="Dapur Sappa" class="biglogo">';
    if (stage === 'code') {
      h += `<h1>Selamat datang</h1><p class="mu">Masukkan kode toko untuk memakai aplikasi</p>
        <input id="code" class="in code" type="password" inputmode="numeric" maxlength="5" autocomplete="off">
        <div id="code-err" class="err" hidden>Kode salah. Coba lagi.</div>
        <button class="btn" data-a="code">Lanjut</button><p class="mu sm">Kode hanya diminta sekali di browser ini.</p>`;
    } else {
      h += `<h1>Kamu siapa?</h1><p class="mu">Pilih nama supaya riwayat tahu siapa yang mencatat.</p><div class="users">`;
      users.forEach((u, i) => { h += `<button class="user" data-a="pick" data-v="${i}"><b>${esc(u.name)}</b><span>${u.role === 'Admin' ? 'Admin · order dan pembukuan' : 'Dapur · belanja dan masak'}</span></button>`; });
      h += '</div><button class="btn o" data-a="devlogin">Masuk sebagai developer</button>';
    }
    h += '</div>';
    $('#app').innerHTML = h;
    const c = $('#code'); if (c) { c.focus(); c.addEventListener('keydown', (e) => { if (e.key === 'Enter') A.code(); }); }
  }

  // ---------- shell ----------
  function tabsFor(me) {
    const all = [['belanja', 'Belanja'], ['order', 'Order'], ['summary', 'Summary'], ['laporan', 'Laporan'], ['menu', 'Menu'], ['spend', 'Pengeluaran'], ['riwayat', 'Riwayat']];
    let t = me.canAdmin ? all : all.filter((x) => ['belanja', 'summary', 'menu', 'spend', 'riwayat'].indexOf(x[0]) >= 0);
    if (me.isDev) t = t.concat([['kelola', 'Kelola user']]);
    return t;
  }
  function shell(me, body) {
    const tabs = tabsFor(me);
    const apk = CFG.GITHUB_USER ? `<a class="lnk" target="_blank" rel="noopener" href="https://github.com/${CFG.GITHUB_USER}/${CFG.GITHUB_REPO}/releases/latest">Unduh APK</a>` : '';
    const online = Store.status === 'Online';
    return `<header class="top"><div class="brand"><img src="${LOGO}" alt=""><b>Dapur Sappa</b></div>
      <div class="right"><span id="clock" class="clk"></span>
      <span class="sync ${online ? 'on' : ''}" title="${esc(Store.status)}">${online ? 'Online' : 'Lokal'}</span>
      <span class="who">${esc(me.name)} · ${esc(me.label)}</span>
      <button class="mini" data-a="theme">Tema</button><button class="mini" data-a="switch">Ganti user</button></div></header>
      <nav class="tabs">${tabs.map((t) => `<button class="tab${st.tab === t[0] && !st.route ? ' on' : ''}" data-a="tab" data-v="${t[0]}">${t[1]}</button>`).join('')}${apk}</nav>
      <main>${body}</main><footer>Dapur Sappa web v${CFG.WEB_VERSION} · ${esc(Store.status)}${Store.lastSync ? ' · sinkron ' + C.timeOf(Store.lastSync) : ''}</footer>`;
  }
  function tickClock() {
    const el = $('#clock'); if (!el) return; const n = new Date();
    el.textContent = C.dayShort(today()) + ' · ' + C.timeOf(n.getTime()) + ' WIB';
  }

  // ---------- Belanja ----------
  function vBelanja(me) {
    const vd = st.belDate || (st.belDate = today());
    const items = D().purchases.filter((p) => p.date === vd);
    const forToday = items.filter((p) => p.kind !== 'BULANAN' && (p.useDate || p.date) === p.date).reduce((s, p) => s + p.price, 0);
    const later = items.filter((p) => p.kind === 'BAHAN' && (p.useDate || p.date) > p.date).reduce((s, p) => s + p.price, 0);
    const monthly = items.filter((p) => p.kind === 'BULANAN').reduce((s, p) => s + p.price, 0);
    let h = banner('Menu hari ini', menuBanner(today()));
    h += `<div class="card"><h3>Catat belanja</h3><p class="mu">Ketik bebas, urutan boleh terbalik. Contoh: ayam dada fillet 36rb 1kg</p>
      <input id="bel-text" class="in" placeholder="selada 3 ikat 9rb" value="${esc(st.f.belText || '')}" autocomplete="off"><div id="bel-prev"></div></div>`;
    h += nav(vd === today() ? 'Belanja hari ini' : 'Belanja', C.dayLong(vd) + (canEdit(me, vd) ? ' · bisa diedit' : ' · hanya lihat'), 'belprev', 'belnext');
    if (!items.length) h += '<p class="mu pad">Belum ada belanja di tanggal ini.</p>';
    items.forEach((p) => {
      let sub = (C.num(p.qty) + ' ' + p.unit).trim();
      if (p.kind === 'KEMASAN' && p.qty > 0) sub += ' · ' + C.rp(Math.round(p.price / p.qty)) + ' per ' + (p.unit || 'pcs');
      if (p.kind === 'BAHAN' && (p.useDate || p.date) !== p.date) sub += ' · untuk ' + C.dayName(p.useDate);
      h += `<div class="card row click" data-a="${canEdit(me, p.date) ? 'editpur' : ''}" data-v="${p.id}"><div><b>${esc(p.name)}</b><div class="mu">${esc(sub)}</div></div><div class="r"><b>${C.rp(p.price)}</b><br>${kindTag(p.kind)}</div></div>`;
    });
    h += `<div class="card soft"><div class="row"><span class="mu">Untuk hari ini</span><b>${C.rp(forToday)}</b></div>${later ? `<div class="row"><span class="mu">Untuk hari lain</span><b>${C.rp(later)}</b></div>` : ''}${monthly ? `<p class="mu sm">Biaya bulanan ${C.rp(monthly)} masuk ke profit bulanan, tidak ke profit harian.</p>` : ''}</div>`;
    return h;
  }
  function belPreview() {
    const box = $('#bel-prev'); const inp = $('#bel-text'); if (!box || !inp) return;
    const p = C.parse(inp.value); st.f.belText = inp.value;
    if (!p) { box.innerHTML = ''; return; }
    const vd = st.belDate || today();
    if (!p.ok) { box.innerHTML = `<p class="warn">${p.price <= 0 ? 'Harga belum terbaca. Tulis harga seperti 36rb atau 36000.' : 'Nama barang belum terbaca.'}</p><button class="btn o" data-a="belmanual">Isi manual</button>`; return; }
    const kind = st.f.bKind || p.kind;
    const use = st.f.bUse || vd;
    const per = p.qty > 0 ? Math.round(p.price / p.qty) : p.price;
    const perTxt = p.unit === 'kg' ? 'Rp' + C.num(p.price / (p.qty * 1000)) + ' per gram' : C.rp(per) + ' per ' + (p.unit || 'satuan');
    let h = `<div class="prev"><small class="okc">Terbaca otomatis</small><b>${esc(p.name)} · ${esc((C.num(p.qty) + ' ' + p.unit).trim())} · ${C.rp(p.price)}</b><div class="mu">Jenis: ${esc(kindLabel(kind).toLowerCase())} · ${esc(perTxt)}</div></div>`;
    h += '<div class="mu">Jenis</div><div class="chips">' + ['BAHAN', 'KEMASAN', 'BULANAN'].map((k) => chip(kindLabel(k), kind === k, 'belkind', k)).join('') + '</div>';
    if (kind === 'BAHAN') {
      h += `<div class="mu">Dipakai untuk tanggal</div><div class="chips">${chip('Hari ini', use === vd, 'beluse', vd)}${chip('Besok', use === C.addDays(vd, 1), 'beluse', C.addDays(vd, 1))}<input type="date" class="in dt" value="${use}" data-chg="beluse"></div>`;
    }
    h += '<div class="btns"><button class="btn o" data-a="belmanual">Ubah</button><button class="btn" data-a="belsave">Simpan</button></div>';
    box.innerHTML = h;
  }
  function draftPurchase(p, kind, vd, use, me) {
    return { t: 'p', id: C.uuid(), date: vd, useDate: kind === 'BAHAN' ? use : vd, raw: st.f.belText || '', name: p.name, qty: p.qty, unit: p.unit, price: p.price, kind, by: me.idx, del: false };
  }

  // ---------- Order ----------
  function vOrder(me) {
    const date = st.orderDate || (st.orderDate = today());
    const orders = D().orders.filter((o) => o.date === date).slice().reverse();
    const pax = orders.reduce((s, o) => s + C.orderPortions(o), 0);
    const omzet = orders.reduce((s, o) => s + C.orderTotal(o), 0);
    const unpaid = orders.filter((o) => !o.paid && C.orderTotal(o) > 0);
    let h = banner(date === today() ? 'Menu hari ini' : 'Menu', menuBanner(date));
    h += nav('Order', C.dayLong(date), 'ordprev', 'ordnext');
    h += `<div class="g3">${tile('Order', orders.length)}${tile('Porsi', pax)}${tile('Omzet', C.rp(omzet), true)}</div>`;
    h += '<h3>Daftar order</h3>';
    if (!orders.length) h += '<p class="mu pad">Belum ada order di tanggal ini.</p>';
    orders.forEach((o) => {
      const sub = (o.location ? o.location + ' · ' : '') + (o.only ? 'tanpa porsi' : o.pax + ' pax');
      h += `<div class="card"><div class="row click" data-a="editorder" data-v="${o.id}"><b>${esc(o.customer)}</b><b>${C.rp(C.orderTotal(o))}</b></div><div class="mu">${esc(sub)}</div>${o.remarks ? `<div class="mu">${esc(o.remarks)}</div>` : ''}${(o.extras || []).map((e) => `<div class="mu">Tambahan: ${esc(e.n)} ${e.q} × ${C.rp(e.p)}</div>`).join('')}
        <div class="tags">${o.only ? tag('Lauk saja', 'ac') : C.extrasTotal(o) > 0 ? tag('+ Tambahan ' + C.rp(C.extrasTotal(o)), 'ac') : ''}${o.paid ? tag('Lunas', 'ok') : tag('Belum bayar', 'wr')}<span class="sp"></span><button class="link" data-a="togglepaid" data-v="${o.id}">${o.paid ? 'Tandai belum' : 'Tandai lunas'}</button></div></div>`;
    });
    if (unpaid.length) h += `<div class="card warnbg"><div class="row"><b>Belum dibayar</b><b>${C.rp(unpaid.reduce((s, o) => s + C.orderTotal(o), 0))}</b></div><div class="mu">${unpaid.length} order belum lunas</div></div>`;
    h += '<button class="btn" data-a="neworder">+ Tambah order</button>';
    return h;
  }
  function orderFormInit(id, date) {
    const old = id ? D().orders.find((o) => o.id === id) : null;
    st.f.of = {
      id: old ? old.id : null, date: old ? old.date : date, customer: old ? old.customer : '', location: old ? old.location : '',
      pax: old ? old.pax : 1, price: String(old ? old.price : C.defaultPrice(D(), date)), remarks: old ? old.remarks : '',
      only: old ? !!old.only : false, paid: old ? !!old.paid : false, extras: old ? JSON.parse(JSON.stringify(old.extras || [])) : [], err: '',
    };
    st.route = { name: 'order' };
  }
  function vOrderForm(me) {
    const f = st.f.of; const price = +f.price || 0; const portions = f.only ? 0 : f.pax;
    const et = f.extras.reduce((s, e) => s + e.q * e.p, 0); const total = portions * price + et;
    let h = `<div class="back"><button class="link" data-a="back">‹ Kembali</button><h2>${f.id ? 'Edit order' : 'Order baru'}</h2><p class="mu">${esc(menuBanner(f.date))}</p></div><div class="card form">`;
    h += `<label class="fld"><span>Tanggal kirim</span><input type="date" class="in" data-bind="of.date" value="${f.date}"></label>`;
    h += field('Nama customer', 'of.customer', f.customer) + field('Lokasi', 'of.location', f.location, 'placeholder="mis. Blok C3 No.5"');
    h += `<div class="mu">Jenis order</div><div class="chips">${chip('Porsi', !f.only, 'ofonly', '0')}${chip('Lauk saja', f.only, 'ofonly', '1')}</div>`;
    if (!f.only) {
      h += `<div class="mu">Jumlah pax</div><div class="step"><button class="arr" data-a="ofpax" data-v="-1">−</button><b>${f.pax}</b><button class="arr" data-a="ofpax" data-v="1">+</button></div>`;
      h += field('Harga per porsi (Rp)', 'of.price', f.price, 'inputmode="numeric" data-num="1"');
    }
    h += `<div class="row"><b>Pesanan tambahan</b><span class="mu">${f.extras.length} item</span></div>`;
    f.extras.forEach((e, i) => { h += `<div class="card inner row"><div><b>${esc(e.n)}</b><div class="mu">${e.q} × ${C.rp(e.p)}${e.c ? ' · ' + esc(e.c) : ''}</div></div><div><b>${C.rp(e.q * e.p)}</b> <button class="link" data-a="ofdelx" data-v="${i}">Hapus</button></div></div>`; });
    h += '<button class="btn o" data-a="ofaddx">+ Tambah pesanan</button>';
    h += field('Remarks', 'of.remarks', f.remarks, 'placeholder="mis. tanpa sambal"');
    h += `<div class="mu">Pembayaran</div><div class="chips">${chip('Lunas', f.paid, 'ofpaid', '1')}${chip('Belum bayar', !f.paid, 'ofpaid', '0')}</div>`;
    h += `<div class="card soft">${f.only ? '' : `<div class="row"><span class="mu">Porsi ${f.pax} × ${C.rp(price)}</span><b>${C.rp(f.pax * price)}</b></div>`}${f.extras.length ? `<div class="row"><span class="mu">Pesanan tambahan (${f.extras.length} item)</span><b>${C.rp(et)}</b></div>` : ''}<div class="row tot"><b>Total order</b><b class="acc">${C.rp(total)}</b></div></div>`;
    if (portions > 0) h += `<p class="mu sm">Kemasan ikut terpotong ${portions} pcs per jenis kemasan.</p>`;
    if (f.err) h += `<p class="err">${esc(f.err)}</p>`;
    h += `<div class="btns"><button class="btn" data-a="ofsave">Simpan order</button><button class="btn o" data-a="back">Batal</button>${f.id ? '<button class="link dng" data-a="ofdel">Hapus order</button>' : ''}</div></div>`;
    return h;
  }

  // ---------- Summary / detail ----------
  function dayCard(d) {
    const closed = d.libur && !d.orders.length;
    const ml = C.menuLines(d.menuName);
    const menuHtml = ml.length ? ml.map((x) => `<div class="mn">${esc(x)}</div>`).join('') : `<div class="mn none">${closed ? 'Libur' : 'Menu belum diisi'}</div>${closed ? '<div class="mu sm">Bisa dibuka untuk PO spesial</div>' : ''}`;
    return `<div class="card click" data-a="detail" data-v="${d.date}"><div class="row top"><div><div class="mu sm">${C.dayName(d.date)} · ${C.dm(d.date)}</div>${menuHtml}</div>${closed ? tag('Libur') : tag(d.pax + ' pax', d.pax ? 'ac' : '')}</div>${d.orders.length || d.cost > 0 ? `<div class="row"><span class="mu">Modal ${C.rp(d.cost)}</span><b class="${d.profit >= 0 ? 'okc' : 'acc'}">Profit ${C.rp(d.profit)}</b></div>` : (closed ? '' : '<div class="mu">Belum ada order</div>')}</div>`;
  }
  function vSummary(me) {
    const ws = st.weekStart || (st.weekStart = C.mondayOf(today()));
    const days = C.week(D(), ws); const sum = (f) => days.reduce((s, d) => s + f(d), 0);
    const end = C.addDays(ws, 6);
    const exps = D().expenses.filter((e) => e.date >= ws && e.date <= end).sort((a, b) => (a.date < b.date ? -1 : 1));
    let h = nav('Summary Week ' + C.weekNo(ws), C.range(ws, end), 'wkprev', 'wknext');
    h += `<div class="g4">${tile('Total pax', sum((d) => d.pax))}${tile('Omzet', C.rp(sum((d) => d.revenue)))}${tile('Modal belanja', C.rp(sum((d) => d.cost)))}${tile('Pengeluaran lain', C.rp(sum((d) => d.expCost)))}</div>`;
    h += tile('Profit bersih', C.rp(sum((d) => d.profit)), true);
    h += '<div class="g2">' + days.map(dayCard).join('') + '</div>';
    h += `<div class="row"><h3>Pengeluaran lain</h3><b>${C.rp(exps.filter((e) => !e.monthly).reduce((s, e) => s + e.amount, 0))}</b></div>`;
    h += exps.length ? '<div class="card">' + exps.map((e) => `<div class="row ln"><div><b>${esc(e.name)}</b><div class="mu">${C.dayName(e.date)} · ${esc(userName(e.by))} · ${e.monthly ? 'Profit Bulanan' : 'Profit Harian'}</div></div><b>${C.rp(e.amount)}</b></div>`).join('') + '</div>' : '<p class="mu">Belum ada pengeluaran lain minggu ini.</p>';
    h += '<button class="btn" data-a="tab" data-v="spend">+ Tambah pengeluaran</button>';
    return h;
  }
  function vDetail(me) {
    const date = st.route.date; const d = C.day(D(), date);
    let h = `<div class="back"><button class="link" data-a="back">‹ Kembali</button><h2>${C.dayLong(date)}</h2><p class="mu">${esc(C.menuLines(d.menuName).join(' · ') || 'Menu belum diisi')} · ${d.pax} pax · ${d.orders.length} order</p></div>`;
    h += `<div class="g3">${tile('Omzet', C.rp(d.revenue))}${tile('Modal', C.rp(d.cost))}${tile('Profit', C.rp(d.profit), true)}</div>`;
    if (d.expCost > 0) h += `<p class="mu">Profit sudah dikurangi pengeluaran lain ${C.rp(d.expCost)} (${esc(d.expenses.map((e) => e.name).join(', '))}).</p>`;
    h += '<h3>Bahan dan kemasan</h3><div class="card"><table><tr><th>Bahan</th><th>Qty</th><th>Harga</th><th>Per porsi</th></tr>';
    if (!d.bahan.length && !d.kemasan.length) h += '<tr><td colspan="4" class="mu">Belum ada belanja untuk hari ini.</td></tr>';
    d.bahan.forEach((p) => { h += `<tr><td>${esc(p.name)}</td><td>${esc((C.num(p.qty) + ' ' + p.unit).trim())}</td><td>${C.th(p.price)}</td><td>${d.pax ? C.th(p.price / d.pax) : '-'}</td></tr>`; });
    d.kemasan.forEach((k) => { h += `<tr><td>${esc(k.name)}</td><td>${k.pcs} pcs</td><td>${C.th(k.cost)}</td><td>${C.th(k.unitPrice)}</td></tr>`; });
    h += `<tr class="tot"><td>Total</td><td></td><td>${C.th(d.cost)}</td><td class="acc">${d.pax ? C.th(d.costPerPax) : '-'}</td></tr></table></div>`;
    if (d.pax) h += `<p class="mu sm">Per porsi = harga ÷ ${d.pax} pax terjual. Biaya bulanan tidak ikut dihitung di sini.</p>`;
    h += '<h3>Order hari ini</h3>';
    if (!d.orders.length) h += '<p class="mu">Belum ada order.</p>';
    d.orders.forEach((o) => { h += `<div class="card row ${me.canAdmin ? 'click' : ''}" ${me.canAdmin ? `data-a="editorder" data-v="${o.id}"` : ''}><div><b>${esc(o.customer)}</b><div class="mu">${esc((o.location ? o.location + ' · ' : '') + (o.only ? 'lauk saja' : o.pax + ' pax') + (C.extrasTotal(o) ? ' · + tambahan ' + C.rp(C.extrasTotal(o)) : ''))}</div></div><b>${C.rp(C.orderTotal(o))}</b></div>`; });
    return h;
  }

  // ---------- Laporan ----------
  function dayRow(d) {
    return `<div class="card row click" data-a="detail" data-v="${d.date}"><div><b>${C.dayShort(d.date)}</b><div class="mu">${esc(C.menuLines(d.menuName).join(' · ') || '-')} · ${d.pax} pax</div></div><div class="r"><b class="${d.profit >= 0 ? 'okc' : 'acc'}">${C.rp(d.profit)}</b><div class="mu">Omzet ${C.rp(d.revenue)} · Modal ${C.rp(d.cost)}</div></div></div>`;
  }
  function vLaporan() {
    const rm = st.repMonth || (st.repMonth = C.firstOfMonth(today()));
    const rw = st.repWeek || (st.repWeek = C.mondayOf(today()));
    let h = `<div class="chips">${chip('Harian', st.repTab === 0, 'reptab', 0)}${chip('Mingguan', st.repTab === 1, 'reptab', 1)}${chip('Bulanan', st.repTab === 2, 'reptab', 2)}</div>`;
    const monthNav = () => {
      const t = today(); const cur = rm.substring(0, 7) === t.substring(0, 7);
      return nav(C.monthName(rm), '1 – ' + (cur ? C.dayNum(t) : C.monthLen(rm)) + ' ' + C.monthAbbr(rm) + (cur ? ' (berjalan)' : ''), 'mprev', 'mnext');
    };
    if (st.repTab === 0) {
      const m = C.month(D(), rm); h += monthNav();
      const act = m.days.filter((d) => d.revenue > 0 || d.cost > 0 || d.expCost > 0).reverse();
      h += act.length ? '<div class="g2">' + act.map(dayRow).join('') + '</div>' : '<p class="mu">Belum ada data di bulan ini.</p>';
      h += '<p class="mu sm">Klik hari untuk melihat rincian bahan dan order.</p>';
    } else if (st.repTab === 1) {
      const days = C.week(D(), rw); const sum = (f) => days.reduce((s, d) => s + f(d), 0);
      h += nav('Week ' + C.weekNo(rw), C.range(rw, C.addDays(rw, 6)), 'rwprev', 'rwnext');
      h += tile('Profit minggu ini', C.rp(sum((d) => d.profit)), true);
      h += `<div class="g3">${tile('Omzet', C.rp(sum((d) => d.revenue)))}${tile('Modal belanja', C.rp(sum((d) => d.cost)))}${tile('Total pax', sum((d) => d.pax))}</div>`;
      h += '<div class="g2">' + days.map(dayRow).join('') + '</div><p class="mu sm">Biaya bulanan tidak dihitung di laporan mingguan.</p>';
    } else {
      const m = C.month(D(), rm); h += monthNav();
      h += tile('Profit bersih bulan ini', C.rp(m.profit), true);
      h += `<div class="g4">${tile('Omzet', C.rp(m.revenue))}${tile('Modal belanja', C.rp(m.modal))}${tile('Pengeluaran lain', C.rp(m.expHarian))}${tile('Biaya bulanan', C.rp(m.monthlyCost))}</div>`;
      h += '<h3>Omzet per minggu</h3><div class="card">';
      const groups = {}; m.days.forEach((d) => { const k = C.mondayOf(d.date); (groups[k] = groups[k] || []).push(d); });
      Object.keys(groups).sort().forEach((k) => {
        const l = groups[k]; h += `<div class="row ln"><div><b>Week ${C.weekNo(k)}</b><div class="mu">${C.dayNum(l[0].date)}–${C.dayNum(l[l.length - 1].date)} ${C.monthAbbr(l[0].date)}</div></div><div class="r"><b>${C.rp(l.reduce((s, d) => s + d.revenue, 0))}</b><div class="mu">Profit ${C.rp(l.reduce((s, d) => s + d.profit, 0))}</div></div></div>`;
      });
      h += `</div><div class="row"><h3>Biaya bulanan</h3><b>${C.rp(m.monthlyCost)}</b></div><div class="card">`;
      h += m.monthlyLines.length ? m.monthlyLines.map((l) => `<div class="row ln"><span>${esc(l[0])}</span><b>${C.th(l[1])}</b></div>`).join('') : '<p class="mu">Belum ada biaya bulanan.</p>';
      h += '</div><p class="mu sm">Biaya bulanan hanya dihitung di laporan bulanan.</p>';
      h += `<div class="card warnbg click" data-a="unpaid"><div class="row"><b>Belum dibayar customer</b><b>${C.rp(m.unpaid.reduce((s, o) => s + C.orderTotal(o), 0))}</b></div><div class="mu">${m.unpaid.length} order · klik untuk ${st.f.unpaid ? 'menutup' : 'melihat rincian'}</div>`;
      if (st.f.unpaid) h += m.unpaid.slice().sort((a, b) => (a.date < b.date ? -1 : 1)).map((o) => `<div class="row ln"><span>${esc(o.customer)} · ${C.dayShort(o.date)}</span><b>${C.rp(C.orderTotal(o))}</b></div>`).join('');
      h += '</div>';
    }
    return h;
  }

  // ---------- Menu ----------
  function menuInit(week) {
    const saved = D().menus[week]; const base = saved || { days: ['', '', '', '', '', '', ''], libur: [false, false, false, false, false, false, true], price: C.defaultPrice(D(), week) };
    const lines = [];
    for (let i = 0; i < 7; i++) { const p = C.menuLines(base.days[i]); for (let j = 0; j < 3; j++) lines.push(p[j] || ''); }
    st.f.mn = { week, lines, libur: base.libur.slice(), price: String(base.price), msg: '' };
  }
  function vMenu(me) {
    if (!st.f.mn) menuInit(C.mondayOf(today()));
    const f = st.f.mn; const ed = me.canAdmin;
    let h = nav('Week ' + C.weekNo(f.week), C.range(f.week, C.addDays(f.week, 6)), 'mnprev', 'mnnext');
    h += '<div class="card">';
    for (let i = 0; i < 7; i++) {
      const dt = C.addDays(f.week, i);
      h += `<div class="mrow"><div class="dn"><b>${C.DAYS[i]}</b><div class="mu sm">${C.dm(dt)}</div></div><div class="mlines">${[0, 1, 2].map((j) => `<input class="in" data-bind="mn.l${i * 3 + j}" value="${esc(f.lines[i * 3 + j])}" placeholder="${j === 0 && i === 6 ? 'Menu PO spesial' : ['Nasi / karbo', 'Lauk utama', 'Sayur / pendamping'][j]}" ${ed ? '' : 'disabled'}>`).join('')}</div>${chip('Libur', f.libur[i], ed ? 'mnlibur' : '', i)}</div>`;
    }
    h += '</div>' + banner('Minggu biasanya libur', 'Untuk PO menu spesial, matikan Libur dan isi menunya. Hari itu tampil seperti hari biasa.');
    h += field('Harga jual per porsi (Rp)', 'mn.price', f.price, `inputmode="numeric" data-num="1" ${ed ? '' : 'disabled'}`) + '<p class="mu">Harga bisa diubah per order saat mencatat order.</p>';
    if (!ed) h += '<p class="warn">Hanya admin yang bisa mengubah menu.</p>';
    if (f.msg) h += `<p class="okc">${esc(f.msg)}</p>`;
    if (ed) h += '<div class="btns"><button class="btn" data-a="mnsave">Simpan menu</button><button class="btn o" data-a="mncopy">Salin dari minggu lalu</button></div>';
    return h;
  }

  // ---------- Pengeluaran ----------
  function vSpend(me) {
    const f = st.f.sp || (st.f.sp = { kind: 'Bensin', desc: 'Bensin motor antar', amount: '', date: today(), monthly: false, err: '' });
    const mon = C.mondayOf(f.date);
    const list = D().expenses.filter((e) => e.date >= mon && e.date <= C.addDays(mon, 6)).sort((a, b) => (a.date < b.date ? -1 : 1));
    let h = '<h2>Pengeluaran lain</h2><div class="card form"><div class="mu">Jenis</div><div class="chips">' + ['Bensin', 'Tambal ban', 'Parkir', 'Servis motor', 'Lainnya'].map((k) => chip(k, f.kind === k, 'spkind', k)).join('') + '</div>';
    h += field('Keterangan', 'sp.desc', f.desc) + field('Nominal (Rp)', 'sp.amount', f.amount, 'inputmode="numeric" data-num="1"');
    h += `<label class="fld"><span>Tanggal</span><input type="date" class="in" data-bind="sp.date" data-chg="spdate" value="${f.date}"></label>`;
    h += `<div class="mu">Charge ke</div><div class="chips">${chip('Profit Harian', !f.monthly, 'spmon', '0')}${chip('Profit Bulanan', f.monthly, 'spmon', '1')}</div>`;
    h += '<p class="mu sm">Profit Harian: mengurangi profit hari itu, lalu ikut ke mingguan dan bulanan. Profit Bulanan: hanya mengurangi profit bulanan.</p>';
    if (f.err) h += `<p class="err">${esc(f.err)}</p>`;
    h += '<button class="btn" data-a="spsave">Simpan</button></div><h3>Pengeluaran minggu ini</h3>';
    if (!list.length) h += '<p class="mu">Belum ada.</p>';
    list.forEach((e) => { h += `<div class="card row"><div><b>${esc(e.name)}</b><div class="mu">${C.dayName(e.date)} · ${esc(userName(e.by))}</div></div><div class="r"><b>${C.rp(e.amount)}</b><br>${e.monthly ? tag('Bulanan', 'wr') : tag('Harian', 'ok')}${canEdit(me, e.date) ? ` <button class="link" data-a="spdel" data-v="${e.id}">Hapus</button>` : ''}</div></div>`; });
    return h;
  }

  // ---------- Riwayat ----------
  function vRiwayat() {
    const filters = ['Semua', 'Belanja', 'Order', 'Pengeluaran', 'Menu', 'User'];
    let h = '<h2>Riwayat perubahan</h2><p class="mu">Siapa mengubah apa, dan kapan</p><div class="chips">' + filters.map((f) => chip(f, st.rf === f, 'rf', f)).join('') + '</div>';
    const list = D().audits.filter((a) => st.rf === 'Semua' || a.cat === st.rf).slice(0, 300);
    if (!list.length) h += '<p class="mu">Belum ada riwayat.</p>';
    let last = null; const t = today(), y = C.addDays(t, -1);
    list.forEach((a) => {
      const d = C.dateOfTs(a.ts);
      if (d !== last) { last = d; h += `<h4>${d === t ? 'Hari ini · ' + C.dayShort(d) : d === y ? 'Kemarin · ' + C.dayShort(d) : C.dayLong(d)}</h4>`; }
      h += `<div class="card"><div class="row"><b>${esc(a.title)}</b><span class="mu">${C.timeOf(a.ts)}</span></div><div class="mu">${esc(a.who)}</div>${a.detail ? `<div class="det">${esc(a.detail)}</div>` : ''}</div>`;
    });
    return h + '<p class="mu sm">Data yang dihapus tetap tercatat di sini.</p>';
  }

  // ---------- Kelola user ----------
  function vKelola() {
    if (!st.f.ku) st.f.ku = { users: D().users.map((u) => ({ name: u.name, role: u.role })), devUser: devUser(), project: Store.project(), key: Store.apiKey(), msg: '' };
    const f = st.f.ku;
    let h = '<h2>Kelola user</h2><p class="mu">Hanya developer</p>';
    f.users.forEach((u, i) => { h += `<div class="card form">${field('Nama User ' + (i + 1), 'ku.n' + i, u.name)}<div class="chips">${chip('Dapur', u.role === 'Dapur', 'kurole', i + ':Dapur')}${chip('Admin', u.role === 'Admin', 'kurole', i + ':Admin')}</div></div>`; });
    h += '<button class="btn o" data-a="kuadd">+ Tambah user</button><h3>Keamanan</h3>';
    h += '<div class="card row"><div><b>Kode toko</b><div class="mu">5 digit · diminta sekali per browser</div></div><b>•••••</b></div>';
    h += `<div class="card form"><b>Akun developer</b><div class="mu">Masuk dengan username dan password</div>${field('Username developer', 'ku.devUser', f.devUser)}<button class="btn o" data-a="kupass">Ganti password developer</button></div>`;
    h += `<h3>Sinkron online (Firebase)</h3><div class="card form">${field('Project ID', 'ku.project', f.project)}${field('Web API key', 'ku.key', f.key)}<div class="mu">Status: ${esc(Store.status)}</div></div>`;
    if (f.msg) h += `<p class="okc">${esc(f.msg)}</p>`;
    return h + '<button class="btn" data-a="kusave">Simpan perubahan</button>';
  }

  // ---------- modal ----------
  function openModal(html) { const m = $('#modal'); m.innerHTML = `<div class="ovl"><div class="dlg">${html}</div></div>`; st.modal = true; }
  function closeModal() { $('#modal').innerHTML = ''; st.modal = false; st.m = null; }
  function purModal() {
    const m = st.m; const kinds = ['BAHAN', 'KEMASAN', 'BULANAN'];
    openModal(`<h3>${m.isNew ? 'Ubah sebelum simpan' : 'Edit belanja'}</h3>${field('Nama barang', 'm.name', m.name)}<div class="two">${field('Jumlah', 'm.qty', m.qty)}${field('Satuan', 'm.unit', m.unit)}</div>${field('Harga total (Rp)', 'm.price', m.price, 'inputmode="numeric" data-num="1"')}
      <div class="mu">Jenis</div><div class="chips">${kinds.map((k) => chip(kindLabel(k), m.kind === k, 'mkind', k)).join('')}</div>
      ${m.kind === 'BAHAN' ? `<label class="fld"><span>Dipakai untuk tanggal</span><input type="date" class="in" data-bind="m.use" value="${m.use}"></label>` : ''}
      ${m.err ? `<p class="err">${esc(m.err)}</p>` : ''}<div class="btns"><button class="btn" data-a="msave">Simpan</button>${m.isNew ? '' : '<button class="btn o" data-a="mdel">Hapus</button>'}<button class="btn o" data-a="mcancel">Batal</button></div>`);
  }
  function extraModal() {
    const m = st.m;
    openModal(`<h3>Tambah pesanan</h3><div class="mu">Pilih cepat</div><div class="chips">${['Lauk tambahan', 'Nasi tambahan', 'Sambal'].map((n) => chip(n, m.n === n, 'xquick', n)).join('')}${chip('Lainnya', false, 'xquick', '')}</div>
      ${field('Tambah apa', 'm.n', m.n)}<div class="mu">Jumlah</div><div class="step"><button class="arr" data-a="xqty" data-v="-1">−</button><b>${m.q}</b><button class="arr" data-a="xqty" data-v="1">+</button></div>
      ${field('Harga satuan (Rp)', 'm.p', m.p, 'inputmode="numeric" data-num="1"')}${field('Catatan (opsional)', 'm.c', m.c)}
      <div class="row"><span class="mu">Total tambahan</span><b>${C.rp(m.q * (+m.p || 0))}</b></div><p class="mu sm">Masuk ke omzet hari itu.</p>
      ${m.err ? `<p class="err">${esc(m.err)}</p>` : ''}<div class="btns"><button class="btn" data-a="xsave">Simpan pesanan</button><button class="btn o" data-a="mcancel">Batal</button></div>`);
  }
  function devModal(err) {
    openModal(`<h3>Login developer</h3>${field('Username', 'm.u', '')}<label class="fld"><span>Password</span><input class="in" type="password" data-bind="m.p" id="devpass"></label>${err ? '<p class="err">Username atau password salah.</p>' : ''}<div class="btns"><button class="btn" data-a="devok">Masuk</button><button class="btn o" data-a="mcancel">Batal</button></div>`);
    st.m = st.m || {};
  }
  function passModal(err) {
    openModal(`<h3>Ganti password developer</h3><label class="fld"><span>Password baru</span><input class="in" type="password" data-bind="m.p1"></label><label class="fld"><span>Ulangi password</span><input class="in" type="password" data-bind="m.p2"></label>${err ? `<p class="err">${esc(err)}</p>` : ''}<div class="btns"><button class="btn" data-a="passok">Simpan</button><button class="btn o" data-a="mcancel">Batal</button></div>`);
  }

  // ---------- render ----------
  function body(me) {
    if (st.route) {
      if (st.route.name === 'order') return vOrderForm(me);
      if (st.route.name === 'detail') return vDetail(me);
    }
    switch (st.tab) {
      case 'belanja': return vBelanja(me);
      case 'order': return vOrder(me);
      case 'summary': return vSummary(me);
      case 'laporan': return vLaporan(me);
      case 'menu': return vMenu(me);
      case 'spend': return vSpend(me);
      case 'riwayat': return vRiwayat(me);
      case 'kelola': return vKelola(me);
    }
    return '';
  }
  function render() {
    if (LS.get('ds_code', '') !== '1') return renderAuth('code');
    const me = getMe();
    if (!me) return renderAuth('pick');
    const tabs = tabsFor(me);
    if (!st.tab || !tabs.some((t) => t[0] === st.tab)) st.tab = me.isDev ? 'summary' : me.canAdmin ? 'order' : 'belanja';
    $('#app').innerHTML = shell(me, body(me));
    tickClock();
    if (st.tab === 'belanja' && !st.route) belPreview();
    window.scrollTo(0, 0);
  }
  function softRender() {
    const ae = document.activeElement;
    if (st.modal || (ae && (ae.tagName === 'INPUT' || ae.tagName === 'TEXTAREA' || ae.tagName === 'SELECT'))) {
      const f = $('footer'); if (f) f.textContent = 'Dapur Sappa web v' + CFG.WEB_VERSION + ' · ' + Store.status;
      return;
    }
    const y = window.scrollY; render(); window.scrollTo(0, y);
  }
  function keepRender() { const y = window.scrollY; render(); window.scrollTo(0, y); }

  // ---------- aksi ----------
  const A = {
    code() {
      const v = ($('#code') || {}).value || '';
      if (C.sha256(v) === CFG.CODE_SHA256) { LS.set('ds_code', '1'); render(); } else { const e = $('#code-err'); if (e) e.hidden = false; }
    },
    pick(v) { LS.set('ds_user', String(v)); LS.set('ds_dev', ''); st.tab = null; render(); },
    devlogin() { st.m = {}; devModal(false); },
    devok() {
      const m = st.m || {};
      if ((m.u || '').trim() === devUser() && C.sha256(m.p || '') === devHash()) { LS.set('ds_dev', '1'); closeModal(); st.tab = null; render(); } else devModal(true);
    },
    switch() { LS.set('ds_user', '-1'); LS.set('ds_dev', ''); st.tab = null; st.route = null; render(); },
    theme() { const n = { '': 'light', light: 'dark', dark: '' }[LS.get('ds_theme', '')]; LS.set('ds_theme', n); applyTheme(); },
    tab(v) { st.tab = v; st.route = null; if (v === 'menu') menuInit(C.mondayOf(today())); if (v === 'kelola') st.f.ku = null; render(); },
    back() { st.route = null; keepRender(); },
    detail(v) { st.route = { name: 'detail', date: v }; render(); },
    // belanja
    belprev() { st.belDate = C.addDays(st.belDate, -1); render(); }, belnext() { st.belDate = C.addDays(st.belDate, 1); render(); },
    belkind(v) { st.f.bKind = v; belPreview(); },
    beluse(v, el) { st.f.bUse = (el && el.value) || v; belPreview(); },
    belmanual() {
      const me = getMe(); const p = C.parse((st.f.belText || '')) || { name: '', qty: 1, unit: '', price: 0, kind: 'BAHAN' }; const vd = st.belDate;
      const kind = st.f.bKind || p.kind;
      st.m = { isNew: true, old: null, id: C.uuid(), date: vd, name: p.name, qty: C.num(p.qty), unit: p.unit, price: String(p.price || ''), kind, use: st.f.bUse || vd, raw: st.f.belText || '', err: '' };
      purModal();
    },
    belsave() {
      const me = getMe(); const p = C.parse(st.f.belText || ''); if (!p || !p.ok) return;
      const vd = st.belDate; savePurchase(draftPurchase(p, st.f.bKind || p.kind, vd, st.f.bUse || vd, me), null, me);
      st.f.belText = ''; st.f.bKind = null; st.f.bUse = null; keepRender();
    },
    editpur(v) {
      const p = D().purchases.find((x) => x.id === v); if (!p) return;
      st.m = { isNew: false, old: p, id: p.id, date: p.date, name: p.name, qty: C.num(p.qty), unit: p.unit, price: String(p.price), kind: p.kind, use: p.useDate || p.date, raw: p.raw, err: '' };
      purModal();
    },
    mkind(v) { st.m.kind = v; purModal(); },
    msave() {
      const m = st.m, me = getMe(); const price = +m.price || 0;
      if (!m.name.trim() || price <= 0) { m.err = 'Nama dan harga harus diisi.'; purModal(); return; }
      const rec = { t: 'p', id: m.id, date: m.date, useDate: m.kind === 'BAHAN' ? m.use : m.date, raw: m.raw || '', name: m.name.trim(), qty: parseFloat(String(m.qty).replace(',', '.')) || 1, unit: (m.unit || '').trim(), price, kind: m.kind, by: m.old ? m.old.by : me.idx, del: false };
      savePurchase(rec, m.old, me);
      if (m.isNew) { st.f.belText = ''; st.f.bKind = null; st.f.bUse = null; }
      closeModal(); keepRender();
    },
    mdel() { deletePurchase(st.m.old, getMe()); closeModal(); keepRender(); },
    mcancel() { closeModal(); },
    // order
    ordprev() { st.orderDate = C.addDays(st.orderDate, -1); render(); }, ordnext() { st.orderDate = C.addDays(st.orderDate, 1); render(); },
    neworder() { orderFormInit(null, st.orderDate); render(); },
    editorder(v) { const o = D().orders.find((x) => x.id === v); if (o) { orderFormInit(v, o.date); render(); } },
    togglepaid(v) { const o = D().orders.find((x) => x.id === v); if (o) { saveOrder(Object.assign({}, o, { paid: !o.paid }), o, getMe()); keepRender(); } },
    ofonly(v) { st.f.of.only = v === '1'; keepRender(); },
    ofpaid(v) { st.f.of.paid = v === '1'; keepRender(); },
    ofpax(v) { st.f.of.pax = Math.max(1, st.f.of.pax + +v); keepRender(); },
    ofdelx(v) { st.f.of.extras.splice(+v, 1); keepRender(); },
    ofaddx() { st.m = { n: '', q: 1, p: '', c: '', err: '' }; extraModal(); },
    xquick(v) { st.m.n = v; extraModal(); },
    xqty(v) { st.m.q = Math.max(1, st.m.q + +v); extraModal(); },
    xsave() {
      const m = st.m; const p = +m.p || 0;
      if (!m.n.trim() || p <= 0) { m.err = 'Nama dan harga harus diisi.'; extraModal(); return; }
      st.f.of.extras.push({ n: m.n.trim(), q: m.q, p, c: (m.c || '').trim() }); closeModal(); keepRender();
    },
    ofsave() {
      const f = st.f.of, me = getMe(); const price = +f.price || 0;
      if (!f.customer.trim()) f.err = 'Nama customer harus diisi.';
      else if (!f.only && price <= 0) f.err = 'Harga per porsi harus diisi.';
      else if (f.only && !f.extras.length) f.err = 'Lauk saja butuh minimal 1 pesanan tambahan.';
      else {
        const old = f.id ? D().orders.find((o) => o.id === f.id) : null;
        const o = { t: 'o', id: f.id || C.uuid(), date: f.date, customer: f.customer.trim(), location: f.location.trim(), pax: f.only ? 0 : f.pax, remarks: f.remarks.trim(), price, extras: f.extras, paid: f.paid, only: f.only, by: old ? old.by : me.idx, del: false };
        saveOrder(o, old, me); st.orderDate = f.date; st.route = null; render(); return;
      }
      keepRender();
    },
    ofdel() { const o = D().orders.find((x) => x.id === st.f.of.id); if (o && confirm('Hapus order ' + o.customer + '? Tetap tercatat di Riwayat.')) { deleteOrder(o, getMe()); st.route = null; render(); } },
    // summary / laporan
    wkprev() { st.weekStart = C.addDays(st.weekStart, -7); render(); }, wknext() { st.weekStart = C.addDays(st.weekStart, 7); render(); },
    reptab(v) { st.repTab = +v; render(); },
    mprev() { st.repMonth = C.addMonths(st.repMonth, -1); render(); }, mnext() { st.repMonth = C.addMonths(st.repMonth, 1); render(); },
    rwprev() { st.repWeek = C.addDays(st.repWeek, -7); render(); }, rwnext() { st.repWeek = C.addDays(st.repWeek, 7); render(); },
    unpaid() { st.f.unpaid = !st.f.unpaid; keepRender(); },
    // menu
    mnprev() { menuInit(C.addDays(st.f.mn.week, -7)); render(); }, mnnext() { menuInit(C.addDays(st.f.mn.week, 7)); render(); },
    mnlibur(v) { if (v === '') return; st.f.mn.libur[+v] = !st.f.mn.libur[+v]; keepRender(); },
    mnsave() { const f = st.f.mn; saveMenu({ week: f.week, days: [0, 1, 2, 3, 4, 5, 6].map((i) => [0, 1, 2].map((j) => f.lines[i * 3 + j].trim()).filter((x) => x).join('\n')), libur: f.libur.slice(), price: +f.price || 20000 }, D().menus[f.week], getMe()); f.msg = 'Menu tersimpan.'; keepRender(); },
    mncopy() {
      const f = st.f.mn; const prev = D().menus[C.addDays(f.week, -7)];
      if (prev) { for (let i = 0; i < 7; i++) { const p = C.menuLines(prev.days[i]); for (let j = 0; j < 3; j++) f.lines[i * 3 + j] = p[j] || ''; } f.libur = prev.libur.slice(); f.price = String(prev.price); f.msg = 'Disalin. Tekan Simpan menu untuk menyimpan.'; } else f.msg = 'Minggu lalu belum ada menu.';
      keepRender();
    },
    // pengeluaran
    spkind(v) { const f = st.f.sp; f.kind = v; f.desc = v === 'Bensin' ? 'Bensin motor antar' : v === 'Lainnya' ? '' : v; keepRender(); },
    spmon(v) { st.f.sp.monthly = v === '1'; keepRender(); },
    spdate() { keepRender(); },
    spsave() {
      const f = st.f.sp, me = getMe(); const a = +f.amount || 0;
      if (!f.desc.trim()) f.err = 'Keterangan harus diisi.'; else if (a <= 0) f.err = 'Nominal harus diisi.';
      else { saveExpense({ t: 'e', id: C.uuid(), date: f.date, name: f.desc.trim(), amount: a, monthly: f.monthly, by: me.idx, del: false }, me); f.amount = ''; f.err = ''; }
      keepRender();
    },
    spdel(v) { const e = D().expenses.find((x) => x.id === v); if (e) { deleteExpense(e, getMe()); keepRender(); } },
    rf(v) { st.rf = v; render(); },
    // kelola
    kurole(v) { const p = v.split(':'); st.f.ku.users[+p[0]].role = p[1]; keepRender(); },
    kuadd() { st.f.ku.users.push({ name: 'User ' + (st.f.ku.users.length + 1), role: 'Dapur' }); keepRender(); },
    kupass() { st.m = {}; passModal(''); },
    passok() {
      const m = st.m;
      if ((m.p1 || '').length < 6) return passModal('Minimal 6 karakter.');
      if (m.p1 !== m.p2) return passModal('Password tidak sama.');
      LS.set('ds_devhash', C.sha256(m.p1)); closeModal(); st.f.ku.msg = 'Password developer diganti (berlaku di browser ini).'; keepRender();
    },
    kusave() {
      const f = st.f.ku, me = getMe();
      const users = f.users.map((u, i) => ({ name: (u.name || '').trim() || 'User ' + (i + 1), role: u.role }));
      if (JSON.stringify(users) !== JSON.stringify(D().users)) saveUsers(users, me);
      if (f.devUser.trim()) LS.set('ds_devuser', f.devUser.trim());
      LS.set('ds_project', f.project.trim()); LS.set('ds_key', f.key.trim());
      f.msg = 'Tersimpan.'; Store.kick(); keepRender();
    },
  };

  function bindValue(key, val, el) {
    const parts = key.split('.'); const ns = parts[0], k = parts[1];
    if (el && el.getAttribute('data-num')) { val = digits(val); if (el.value !== val) el.value = val; }
    if (ns === 'of') st.f.of[k] = val;
    else if (ns === 'm') st.m[k] = val;
    else if (ns === 'sp') st.f.sp[k] = val;
    else if (ns === 'mn') { if (/^l\d+$/.test(k)) st.f.mn.lines[+k.substring(1)] = val; else st.f.mn[k] = val; }
    else if (ns === 'ku') { if (/^n\d+$/.test(k)) st.f.ku.users[+k.substring(1)].name = val; else st.f.ku[k] = val; }
    if (ns === 'of' && (k === 'price')) { /* total diperbarui saat render berikutnya */ }
  }

  document.addEventListener('click', (e) => {
    const el = e.target.closest('[data-a]'); if (!el) return;
    const a = el.getAttribute('data-a'); if (!a || !A[a]) return;
    if (el.tagName === 'A') e.preventDefault();
    A[a](el.getAttribute('data-v'), el);
  });
  document.addEventListener('input', (e) => {
    const t = e.target;
    if (t.id === 'bel-text') { st.f.bKind = null; belPreview(); return; }
    const b = t.getAttribute && t.getAttribute('data-bind'); if (b) bindValue(b, t.value, t);
  });
  document.addEventListener('change', (e) => {
    const t = e.target; const c = t.getAttribute && t.getAttribute('data-chg');
    if (c && A[c]) { A[c](t.value, t); return; }
    const b = t.getAttribute && t.getAttribute('data-bind'); if (b) bindValue(b, t.value, t);
  });
  document.addEventListener('keydown', (e) => { if (e.key === 'Enter' && e.target.id === 'devpass') A.devok(); });

  function applyTheme() {
    const t = LS.get('ds_theme', '');
    if (t) document.documentElement.setAttribute('data-theme', t); else document.documentElement.removeAttribute('data-theme');
  }

  // ---------- mulai ----------
  Store.load();
  Store.onChange = (changed) => { if (changed) softRender(); else { const f = $('footer'); if (f) f.textContent = 'Dapur Sappa web v' + CFG.WEB_VERSION + ' · ' + Store.status + (Store.lastSync ? ' · sinkron ' + C.timeOf(Store.lastSync) : ''); } };
  applyTheme();
  render();
  setInterval(tickClock, 30000);
  Store.start();
  window.__ds = { st, A, Store, render }; // untuk pengujian
})();
