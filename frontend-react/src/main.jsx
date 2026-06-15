import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  Activity, BarChart3, Building2, CalendarDays, Database, Home, Loader2,
  MapPin, Ruler, ShieldCheck, Sparkles, TrendingUp, Triangle, Share2,
  Bed, Bath, Wind, GitCompare, Map, Zap, Wallet, ChevronDown, ChevronUp,
  Star, Info, CheckCircle2, ArrowRight, Layers,
} from 'lucide-react';
import './styles.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// ─── constants ───────────────────────────────────────────────────────────────
const FALLBACK_DISTRICTS = [
  'Ba Đình','Bắc Từ Liêm','Cầu Giấy','Đống Đa','Hai Bà Trưng',
  'Hà Đông','Hoàn Kiếm','Hoàng Mai','Long Biên','Nam Từ Liêm','Tây Hồ','Thanh Xuân',
];

const DISTRICT_DISTANCES = {
  'Ba Đình':2.5,'Bắc Từ Liêm':10,'Cầu Giấy':6.5,'Đống Đa':3.5,
  'Hai Bà Trưng':2,'Hà Đông':12,'Hoàn Kiếm':0.5,'Hoàng Mai':7,
  'Long Biên':5.5,'Nam Từ Liêm':9,'Tây Hồ':5,'Thanh Xuân':6,
};

// SVG paths cho 12 quận Hà Nội (simplified polygon approximation)
const DISTRICT_PATHS = {
  'Hoàn Kiếm': 'M195,148 L210,140 L220,152 L208,162 Z',
  'Ba Đình':   'M170,128 L195,120 L202,140 L195,148 L178,148 Z',
  'Tây Hồ':   'M155,90  L185,80  L195,100 L175,118 L158,110 Z',
  'Cầu Giấy': 'M145,130 L170,118 L178,138 L165,150 L148,145 Z',
  'Đống Đa':  'M178,148 L195,148 L200,170 L185,178 L170,168 Z',
  'Hai Bà Trưng':'M210,148 L232,142 L238,165 L218,175 L205,168 Z',
  'Thanh Xuân':'M165,168 L185,178 L190,196 L168,202 L155,188 Z',
  'Hoàng Mai': 'M205,168 L235,165 L242,192 L220,205 L198,198 Z',
  'Nam Từ Liêm':'M132,155 L155,145 L165,168 L148,180 L130,170 Z',
  'Bắc Từ Liêm':'M140,105 L168,95  L175,118 L155,130 L138,122 Z',
  'Hà Đông':  'M130,185 L155,178 L162,200 L140,215 L122,202 Z',
  'Long Biên': 'M235,120 L268,112 L275,145 L252,158 L232,148 Z',
};

const DISTRICT_LABEL_POS = {
  'Hoàn Kiếm': [207,152], 'Ba Đình':[185,135],  'Tây Hồ':[172,98],
  'Cầu Giấy':  [161,137], 'Đống Đa':[185,163],  'Hai Bà Trưng':[222,158],
  'Thanh Xuân':[172,188], 'Hoàng Mai':[220,185], 'Nam Từ Liêm':[146,163],
  'Bắc Từ Liêm':[155,112],'Hà Đông':[140,198],  'Long Biên':[252,133],
};

const FALLBACK_METADATA = {
  districts: FALLBACK_DISTRICTS, trainingRows:0, featureCount:0,
  averagePriceMillion:0, averageArea:0, minPriceMillion:0, maxPriceMillion:0,
  districtAverages:[], priceBuckets:[], areaPricePoints:[], metrics:[],
};

const DEFAULT_FORM = {
  area:25, district:'Ba Đình', day:15, month:6, year:2026,
  numBedrooms:1, numBathrooms:1, hasAirConditioning:0, furnished:0,
  floor:1, roomType:'studio', distanceToCenter:2.5,
};

// ─── formatters ──────────────────────────────────────────────────────────────
function fmt(v, digits = 2) {
  if (v == null || Number.isNaN(Number(v))) return '--';
  return `${Number(v).toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: digits })} triệu`;
}
function fmtNum(v) {
  if (v == null || Number.isNaN(Number(v))) return '--';
  return Number(v).toLocaleString('vi-VN');
}

// ─── small shared components ─────────────────────────────────────────────────
function Stat({ icon, label, value, hint }) {
  return (
    <div className="stat">
      <div className="stat-icon">{icon}</div>
      <div>
        <span className="stat-label">{label}</span>
        <strong className="stat-value">{value}</strong>
        {hint && <p className="stat-hint">{hint}</p>}
      </div>
    </div>
  );
}

function BarChart({ data }) {
  const max = Math.max(...data.map(d => Number(d.averagePrice || 0)), 1);
  return (
    <div className="bar-chart">
      {data.map(d => (
        <div className="bar-row" key={d.district}>
          <span className="bar-label">{d.district}</span>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${(Number(d.averagePrice)/max)*100}%` }} />
          </div>
          <strong>{fmt(d.averagePrice)}</strong>
        </div>
      ))}
    </div>
  );
}

function Histogram({ data }) {
  const max = Math.max(...data.map(d => Number(d.count || 0)), 1);
  return (
    <div className="histogram">
      {data.map(d => (
        <div className="histogram-item" key={d.label}>
          <div className="histogram-bar" style={{ height: `${Math.max(8,(Number(d.count)/max)*150)}px` }} />
          <strong>{d.count}</strong>
          <span>{d.label}</span>
        </div>
      ))}
    </div>
  );
}

function ScatterPlot({ data }) {
  const pts = data || [];
  const maxA = Math.max(...pts.map(p => Number(p.area||0)), 1);
  const maxP = Math.max(...pts.map(p => Number(p.price||0)), 1);
  return (
    <div className="scatter-wrap">
      <svg viewBox="0 0 420 240" role="img" aria-label="Diện tích và giá thuê">
        <line x1="38" y1="18" x2="38" y2="210" className="axis" />
        <line x1="38" y1="210" x2="400" y2="210" className="axis" />
        {[0.25,0.5,0.75].map(r => (
          <line key={r} x1="38" x2="400" y1={210-r*190} y2={210-r*190} className="grid-line" />
        ))}
        {pts.map((p,i) => {
          const x = 38+(Number(p.area)/maxA)*350;
          const y = 210-(Number(p.price)/maxP)*185;
          return <circle key={i} cx={x} cy={y} r="4" className="scatter-dot" />;
        })}
      </svg>
      <div className="scatter-axis"><span>Diện tích →</span><span>Giá →</span></div>
    </div>
  );
}

// ─── FEATURE 3: Heat Map ──────────────────────────────────────────────────────
function HeatMap({ districtAverages }) {
  const [hovered, setHovered] = useState(null);

  const priceMap = useMemo(() => {
    const m = {};
    (districtAverages || []).forEach(d => { m[d.district] = Number(d.averagePrice); });
    return m;
  }, [districtAverages]);

  const prices = Object.values(priceMap).filter(v => v > 0);
  const minP = prices.length ? Math.min(...prices) : 1;
  const maxP = prices.length ? Math.max(...prices) : 10;

  function getColor(district) {
    const p = priceMap[district];
    if (!p) return '#e2e8f0';
    const ratio = (p - minP) / Math.max(maxP - minP, 0.01);
    // Gradient: blue (cheap) → orange → red (expensive)
    const r = Math.round(30 + ratio * 220);
    const g = Math.round(100 - ratio * 60);
    const b = Math.round(180 - ratio * 150);
    return `rgb(${r},${g},${b})`;
  }

  const hoveredData = hovered ? { district: hovered, price: priceMap[hovered] } : null;

  return (
    <div className="heatmap-wrapper">
      <svg viewBox="0 100 320 160" className="heatmap-svg" role="img" aria-label="Bản đồ nhiệt giá thuê Hà Nội">
        {Object.entries(DISTRICT_PATHS).map(([district, path]) => (
          <path
            key={district}
            d={path}
            fill={getColor(district)}
            stroke="#fff"
            strokeWidth="1.5"
            className={`district-path ${hovered === district ? 'district-hovered' : ''}`}
            onMouseEnter={() => setHovered(district)}
            onMouseLeave={() => setHovered(null)}
          />
        ))}
        {Object.entries(DISTRICT_LABEL_POS).map(([district, [x, y]]) => (
          <text
            key={district}
            x={x} y={y}
            className="district-map-label"
            textAnchor="middle"
            fontSize="5.5"
          >
            {district.split(' ')[0]}
          </text>
        ))}
      </svg>

      <div className="heatmap-legend">
        <span>{fmt(minP)}</span>
        <div className="heatmap-gradient" />
        <span>{fmt(maxP)}</span>
      </div>

      {hoveredData && (
        <div className="heatmap-tooltip">
          <strong>{hoveredData.district}</strong>
          <span>{hoveredData.price ? `TB: ${fmt(hoveredData.price)}` : 'Chưa có dữ liệu'}</span>
        </div>
      )}

      {!hoveredData && (
        <p className="heatmap-hint">Di chuột lên bản đồ để xem giá trung bình từng quận</p>
      )}
    </div>
  );
}

// ─── FEATURE 4: Feature Importance ───────────────────────────────────────────
function FeatureImportanceChart({ data, title }) {
  if (!data || data.length === 0) return <div className="empty-chart">Chưa có dữ liệu</div>;
  const top = data.slice(0, 8);
  const maxVal = Math.max(...top.map(d => d.importance), 0.01);
  const colors = ['#123c69','#1e5a9e','#2f6690','#3d7ab5','#5592c8','#7aaed8','#9fc6e5','#c3ddf0'];

  return (
    <div className="fi-chart">
      <p className="fi-title">{title}</p>
      {top.map((d, i) => (
        <div className="fi-row" key={d.feature}>
          <span className="fi-label" title={d.featureVi}>{d.featureVi}</span>
          <div className="fi-track">
            <div
              className="fi-bar"
              style={{ width: `${(d.importance / maxVal) * 100}%`, background: colors[i] }}
            />
          </div>
          <span className="fi-pct">{(d.importance * 100).toFixed(0)}%</span>
        </div>
      ))}
    </div>
  );
}

function FeatureImportancePanel() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [activeModel, setActiveModel] = useState('rf');
  const [open, setOpen] = useState(false);

  function load() {
    if (data) { setOpen(o => !o); return; }
    setLoading(true);
    setOpen(true);
    fetch(`${API_BASE}/feature-importance`)
      .then(r => r.json())
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }

  const modelOptions = [
    { key: 'rf', label: 'Random Forest' },
    { key: 'xgboost', label: 'XGBoost' },
    { key: 'linear', label: 'Linear' },
    { key: 'nn', label: 'Neural Net' },
  ];

  const activeData = data ? data[activeModel] : null;
  const activeTitle = modelOptions.find(m => m.key === activeModel)?.label;

  return (
    <div className="panel fi-panel">
      <button className="fi-toggle" onClick={load} aria-expanded={open}>
        <div className="fi-toggle-left">
          <Zap size={18} />
          <div>
            <strong>Feature Importance</strong>
            <span>Yếu tố nào ảnh hưởng nhiều nhất đến giá?</span>
          </div>
        </div>
        {open ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
      </button>

      {open && (
        <div className="fi-body">
          <div className="fi-model-tabs">
            {modelOptions.map(m => (
              <button
                key={m.key}
                className={`fi-tab ${activeModel === m.key ? 'active' : ''}`}
                onClick={() => setActiveModel(m.key)}
              >
                {m.label}
              </button>
            ))}
          </div>
          {loading && <div className="fi-loading"><Loader2 className="spin" size={20} /> Đang tính...</div>}
          {!loading && activeData && <FeatureImportanceChart data={activeData} title={`Top features — ${activeTitle}`} />}
          {!loading && !activeData && data && <div className="empty-chart">Không có dữ liệu cho model này</div>}
        </div>
      )}
    </div>
  );
}

// ─── helpers ─────────────────────────────────────────────────────────────────
function buildPayload(form) {
  return {
    area: Number(form.area), district: form.district,
    day: Number(form.day), month: Number(form.month), year: Number(form.year),
    numBedrooms: Number(form.numBedrooms), numBathrooms: Number(form.numBathrooms),
    hasAirConditioning: Number(form.hasAirConditioning), furnished: Number(form.furnished),
    floor: Number(form.floor), roomType: form.roomType,
    distanceToCenter: Number(form.distanceToCenter),
  };
}

const ROOM_TYPE_LABEL = { studio: 'Studio', apartment: 'Căn hộ', house: 'Nhà riêng' };

// ─── FEATURE 2: Compare ───────────────────────────────────────────────────────
function CompareFormB({ form, onChange, districts }) {
  function upd(field, value) {
    if (field === 'district') onChange({ ...form, district: value, distanceToCenter: DISTRICT_DISTANCES[value] || 5 });
    else onChange({ ...form, [field]: value });
  }
  return (
    <div className="cmp-form-b">
      {/* Row 1 */}
      <div className="cmp-field-grid">
        <div className="cmp-field">
          <label className="cmp-field-label"><Ruler size={14}/> Diện tích</label>
          <div className="input-with-unit">
            <input type="number" min="5" max="500" value={form.area}
              onChange={e => upd('area', e.target.value)} className="cmp-input"/>
            <em>m²</em>
          </div>
        </div>
        <div className="cmp-field">
          <label className="cmp-field-label"><MapPin size={14}/> Khu vực / Quận</label>
          <select value={form.district} onChange={e => upd('district', e.target.value)} className="cmp-input">
            {districts.map(d => <option key={d} value={d}>{d}</option>)}
          </select>
        </div>
        <div className="cmp-field">
          <label className="cmp-field-label"><Building2 size={14}/> Loại phòng</label>
          <select value={form.roomType} onChange={e => upd('roomType', e.target.value)} className="cmp-input">
            <option value="studio">Studio / Phòng trọ</option>
            <option value="apartment">Căn hộ chung cư</option>
            <option value="house">Nhà nguyên căn</option>
          </select>
        </div>
      </div>
      {/* Row 2 */}
      <div className="cmp-field-grid">
        <div className="cmp-field">
          <label className="cmp-field-label"><Bed size={14}/> Phòng ngủ</label>
          <input type="number" min="1" max="10" value={form.numBedrooms}
            onChange={e => upd('numBedrooms', e.target.value)} className="cmp-input"/>
        </div>
        <div className="cmp-field">
          <label className="cmp-field-label"><Bath size={14}/> Phòng tắm</label>
          <input type="number" min="1" max="10" value={form.numBathrooms}
            onChange={e => upd('numBathrooms', e.target.value)} className="cmp-input"/>
        </div>
        <div className="cmp-field">
          <label className="cmp-field-label">Tầng</label>
          <input type="number" min="1" max="100" value={form.floor}
            onChange={e => upd('floor', e.target.value)} className="cmp-input"/>
        </div>
      </div>
      {/* Row 3: checkboxes */}
      <div className="cmp-checks">
        <label className="cmp-check-item">
          <input type="checkbox" checked={form.hasAirConditioning === 1}
            onChange={e => upd('hasAirConditioning', e.target.checked ? 1 : 0)}/>
          <Wind size={15}/> Có điều hòa
        </label>
        <label className="cmp-check-item">
          <input type="checkbox" checked={form.furnished === 1}
            onChange={e => upd('furnished', e.target.checked ? 1 : 0)}/>
          <Layers size={15}/> Có nội thất
        </label>
        <div className="cmp-field cmp-field-dist">
          <label className="cmp-field-label"><MapPin size={14}/> Cách TT</label>
          <div className="input-with-unit">
            <input type="number" min="0" max="100" step="0.5" value={form.distanceToCenter}
              onChange={e => upd('distanceToCenter', e.target.value)} className="cmp-input"/>
            <em>km</em>
          </div>
        </div>
      </div>
    </div>
  );
}

// Một dòng so sánh tiêu chí
function CmpRow({ label, valA, valB, winnerFn }) {
  const win = winnerFn ? winnerFn(valA, valB) : null; // 'A' | 'B' | null
  return (
    <div className="cmp-row">
      <div className={`cmp-cell cmp-cell-a ${win === 'A' ? 'cmp-cell-win' : ''}`}>
        {win === 'A' && <span className="cmp-win-dot"/>}{valA}
      </div>
      <div className="cmp-row-label">{label}</div>
      <div className={`cmp-cell cmp-cell-b ${win === 'B' ? 'cmp-cell-win' : ''}`}>
        {win === 'B' && <span className="cmp-win-dot"/>}{valB}
      </div>
    </div>
  );
}

function CompareSummary({ formA, formB, predA, predB, selectedModel }) {
  const pA = predA?.predictions?.[selectedModel];
  const pB = predB?.predictions?.[selectedModel];
  if (!pA || !pB) return null;

  const priceA = pA.predictedPriceMillion;
  const priceB = pB.predictedPriceMillion;
  const sqmA   = priceA / Number(formA.area);
  const sqmB   = priceB / Number(formB.area);
  const diff   = priceA - priceB;

  // score: mỗi tiêu chí thắng = 1 điểm
  let scoreA = 0, scoreB = 0;
  if (sqmA < sqmB) scoreA++; else if (sqmB < sqmA) scoreB++;
  if (priceA < priceB) scoreA++; else if (priceB < priceA) scoreB++;
  if (Number(formA.area) > Number(formB.area)) scoreA++; else if (Number(formB.area) > Number(formA.area)) scoreB++;
  if (DISTRICT_DISTANCES[formA.district] < DISTRICT_DISTANCES[formB.district]) scoreA++;
  else if (DISTRICT_DISTANCES[formB.district] < DISTRICT_DISTANCES[formA.district]) scoreB++;

  const overallWinner = scoreA > scoreB ? 'A' : scoreB > scoreA ? 'B' : null;

  return (
    <div className="cmp-summary">
      {/* Header cards */}
      <div className="cmp-header-row">
        <div className={`cmp-header-card cmp-header-a ${overallWinner==='A'?'cmp-overall-win':''}`}>
          <div className="cmp-header-badge cmp-badge-a">
            <Home size={14}/> Căn của tôi
          </div>
          <div className="cmp-header-price">{fmt(priceA)}</div>
          <div className="cmp-header-sub">{formA.area} m² · {formA.district}</div>
          <div className="cmp-header-sub">{ROOM_TYPE_LABEL[formA.roomType] || formA.roomType}</div>
          {overallWinner === 'A' && (
            <div className="cmp-overall-badge"><Star size={13}/> Lựa chọn tốt hơn</div>
          )}
        </div>

        <div className="cmp-header-vs">
          <div className="cmp-vs-circle">VS</div>
          <div className={`cmp-diff-pill ${diff < 0 ? 'cmp-diff-cheaper' : diff > 0 ? 'cmp-diff-pricier' : ''}`}>
            {diff === 0 ? 'Bằng nhau'
              : diff > 0
                ? `Căn tôi đắt hơn ${fmt(Math.abs(diff))}`
                : `Căn tôi rẻ hơn ${fmt(Math.abs(diff))}`}
            /tháng
          </div>
        </div>

        <div className={`cmp-header-card cmp-header-b ${overallWinner==='B'?'cmp-overall-win':''}`}>
          <div className="cmp-header-badge cmp-badge-b">
            <GitCompare size={14}/> Căn so sánh
          </div>
          <div className="cmp-header-price">{fmt(priceB)}</div>
          <div className="cmp-header-sub">{formB.area} m² · {formB.district}</div>
          <div className="cmp-header-sub">{ROOM_TYPE_LABEL[formB.roomType] || formB.roomType}</div>
          {overallWinner === 'B' && (
            <div className="cmp-overall-badge"><Star size={13}/> Lựa chọn tốt hơn</div>
          )}
        </div>
      </div>

      {/* Detail table */}
      <div className="cmp-table">
        <div className="cmp-table-header">
          <div className="cmp-th cmp-th-a">Căn của tôi</div>
          <div className="cmp-th cmp-th-mid">Tiêu chí</div>
          <div className="cmp-th cmp-th-b">Căn so sánh</div>
        </div>

        <CmpRow label="Giá dự đoán"
          valA={fmt(priceA)} valB={fmt(priceB)}
          winnerFn={(a,b) => priceA < priceB ? 'A' : priceA > priceB ? 'B' : null}/>
        <CmpRow label="Khoảng dao động"
          valA={`${fmt(pA.lowerBoundMillion)} – ${fmt(pA.upperBoundMillion)}`}
          valB={`${fmt(pB.lowerBoundMillion)} – ${fmt(pB.upperBoundMillion)}`}
          winnerFn={null}/>
        <CmpRow label="Giá / m²"
          valA={`${(sqmA*1000).toFixed(0)}k/m²`}
          valB={`${(sqmB*1000).toFixed(0)}k/m²`}
          winnerFn={() => sqmA < sqmB ? 'A' : sqmA > sqmB ? 'B' : null}/>
        <CmpRow label="Diện tích"
          valA={`${formA.area} m²`} valB={`${formB.area} m²`}
          winnerFn={() => Number(formA.area) > Number(formB.area) ? 'A' : Number(formB.area) > Number(formA.area) ? 'B' : null}/>
        <CmpRow label="Khu vực"
          valA={formA.district} valB={formB.district} winnerFn={null}/>
        <CmpRow label="Cách trung tâm"
          valA={`${DISTRICT_DISTANCES[formA.district] ?? formA.distanceToCenter} km`}
          valB={`${DISTRICT_DISTANCES[formB.district] ?? formB.distanceToCenter} km`}
          winnerFn={() => {
            const dA = DISTRICT_DISTANCES[formA.district] ?? formA.distanceToCenter;
            const dB = DISTRICT_DISTANCES[formB.district] ?? formB.distanceToCenter;
            return dA < dB ? 'A' : dA > dB ? 'B' : null;
          }}/>
        <CmpRow label="Loại phòng"
          valA={ROOM_TYPE_LABEL[formA.roomType]||formA.roomType}
          valB={ROOM_TYPE_LABEL[formB.roomType]||formB.roomType} winnerFn={null}/>
        <CmpRow label="Phòng ngủ"
          valA={`${formA.numBedrooms} phòng`} valB={`${formB.numBedrooms} phòng`}
          winnerFn={() => Number(formA.numBedrooms) > Number(formB.numBedrooms) ? 'A' : Number(formB.numBedrooms) > Number(formA.numBedrooms) ? 'B' : null}/>
        <CmpRow label="Phòng tắm"
          valA={`${formA.numBathrooms} phòng`} valB={`${formB.numBathrooms} phòng`}
          winnerFn={() => Number(formA.numBathrooms) > Number(formB.numBathrooms) ? 'A' : Number(formB.numBathrooms) > Number(formA.numBathrooms) ? 'B' : null}/>
        <CmpRow label="Điều hòa"
          valA={formA.hasAirConditioning ? '✅ Có' : '❌ Không'}
          valB={formB.hasAirConditioning ? '✅ Có' : '❌ Không'}
          winnerFn={() => formA.hasAirConditioning > formB.hasAirConditioning ? 'A' : formB.hasAirConditioning > formA.hasAirConditioning ? 'B' : null}/>
        <CmpRow label="Nội thất"
          valA={formA.furnished ? '✅ Có' : '❌ Không'}
          valB={formB.furnished ? '✅ Có' : '❌ Không'}
          winnerFn={() => formA.furnished > formB.furnished ? 'A' : formB.furnished > formA.furnished ? 'B' : null}/>
        <CmpRow label="Tầng"
          valA={`Tầng ${formA.floor}`} valB={`Tầng ${formB.floor}`} winnerFn={null}/>
      </div>

      {/* Score bar */}
      <div className="cmp-score">
        <div className="cmp-score-bar-wrap">
          <div className="cmp-score-label cmp-score-label-a">Căn tôi: {scoreA} điểm</div>
          <div className="cmp-score-track">
            <div className="cmp-score-fill-a" style={{width: `${(scoreA/(scoreA+scoreB||1))*100}%`}}/>
            <div className="cmp-score-fill-b" style={{width: `${(scoreB/(scoreA+scoreB||1))*100}%`}}/>
          </div>
          <div className="cmp-score-label cmp-score-label-b">Căn so sánh: {scoreB} điểm</div>
        </div>
      </div>
    </div>
  );
}

function ComparePanel({ districts, myForm, myPrediction }) {
  const defaultB = { ...DEFAULT_FORM, district: 'Hà Đông', area: 35, distanceToCenter: 12, roomType: 'apartment', numBedrooms: 2 };
  const [formB, setFormB] = useState(defaultB);
  const [predB, setPredB] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedModel, setSelectedModel] = useState('rf');
  const [step, setStep] = useState(1); // 1 = nhập căn B, 2 = kết quả

  // Nếu myForm chưa được dự đoán thì predA = null, cần tự fetch
  const [predA, setPredA] = useState(null);

  // Sync predA khi myPrediction thay đổi
  useEffect(() => { if (myPrediction) setPredA(myPrediction); }, [myPrediction]);

  async function runCompare() {
    setLoading(true); setError('');
    try {
      // Luôn fetch cả A để có kết quả mới nhất
      const [rA, rB] = await Promise.all([
        fetch(`${API_BASE}/predict`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(buildPayload(myForm)) }).then(r=>r.json()),
        fetch(`${API_BASE}/predict`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(buildPayload(formB)) }).then(r=>r.json()),
      ]);
      setPredA(rA); setPredB(rB);
      setStep(2);
    } catch { setError('Không thể so sánh. Kiểm tra backend đang chạy.'); }
    setLoading(false);
  }

  const modelOptions = [
    { key:'rf', label:'Random Forest' }, { key:'xgboost', label:'XGBoost' },
    { key:'linear', label:'Linear' },    { key:'nn', label:'Neural Net' },
  ];

  return (
    <div className="panel compare-panel">

      {/* ── Step indicator ── */}
      <div className="cmp-steps">
        <div className={`cmp-step ${step >= 1 ? 'cmp-step-done' : ''}`}>
          <div className="cmp-step-num">1</div>
          <div className="cmp-step-text">
            <strong>Căn của tôi</strong>
            <span>Từ tab Dự đoán giá</span>
          </div>
        </div>
        <div className="cmp-step-line"/>
        <div className={`cmp-step ${step >= 1 ? 'cmp-step-active' : ''}`}>
          <div className="cmp-step-num">2</div>
          <div className="cmp-step-text">
            <strong>Căn cần so sánh</strong>
            <span>Nhập thông tin bên dưới</span>
          </div>
        </div>
        <div className="cmp-step-line"/>
        <div className={`cmp-step ${step >= 2 ? 'cmp-step-done' : ''}`}>
          <div className="cmp-step-num">3</div>
          <div className="cmp-step-text">
            <strong>Kết quả</strong>
            <span>So sánh chi tiết</span>
          </div>
        </div>
      </div>

      {/* ── Căn A preview ── */}
      <div className="cmp-my-room">
        <div className="cmp-my-room-header">
          <Home size={16}/>
          <strong>Căn của tôi</strong>
          <span className="cmp-my-room-hint">(nhập tại tab "Dự đoán giá" — tự động cập nhật)</span>
        </div>
        <div className="cmp-my-room-chips">
          <span className="cmp-chip"><Ruler size={12}/> {myForm.area} m²</span>
          <span className="cmp-chip"><MapPin size={12}/> {myForm.district}</span>
          <span className="cmp-chip"><Building2 size={12}/> {ROOM_TYPE_LABEL[myForm.roomType]||myForm.roomType}</span>
          <span className="cmp-chip"><Bed size={12}/> {myForm.numBedrooms} PN</span>
          <span className="cmp-chip"><Bath size={12}/> {myForm.numBathrooms} PT</span>
          {myForm.hasAirConditioning ? <span className="cmp-chip cmp-chip-green"><Wind size={12}/> Điều hòa</span> : null}
          {myForm.furnished         ? <span className="cmp-chip cmp-chip-green"><Layers size={12}/> Nội thất</span> : null}
          <span className="cmp-chip">Tầng {myForm.floor}</span>
        </div>
      </div>

      {/* ── Divider ── */}
      <div className="cmp-section-divider">
        <div className="cmp-divider-line"/>
        <span>Nhập thông tin căn cần so sánh</span>
        <div className="cmp-divider-line"/>
      </div>

      {/* ── Form B ── */}
      <CompareFormB form={formB} onChange={setFormB} districts={districts}/>

      {/* ── Model selector ── */}
      <div className="cmp-model-row">
        <span className="cmp-model-label"><Database size={14}/> Model:</span>
        {modelOptions.map(m => (
          <button key={m.key} className={`fi-tab ${selectedModel===m.key?'active':''}`}
            onClick={() => setSelectedModel(m.key)}>{m.label}</button>
        ))}
      </div>

      {error && <div className="error" style={{marginTop:12}}>{error}</div>}

      <button className="primary-button" style={{marginTop:16}} onClick={runCompare} disabled={loading}>
        {loading ? <Loader2 className="spin" size={18}/> : <GitCompare size={18}/>}
        So sánh 2 căn
      </button>

      {/* ── Results ── */}
      {step === 2 && predA && predB && (
        <>
          <div className="cmp-result-header">
            <div className="cmp-result-model-label">Kết quả theo model: <strong>{modelOptions.find(m=>m.key===selectedModel)?.label}</strong></div>
            <div className="fi-model-tabs" style={{marginTop:8}}>
              {modelOptions.map(m => (
                <button key={m.key} className={`fi-tab ${selectedModel===m.key?'active':''}`}
                  onClick={() => setSelectedModel(m.key)}>{m.label}</button>
              ))}
            </div>
          </div>
          <CompareSummary formA={myForm} formB={formB} predA={predA} predB={predB} selectedModel={selectedModel}/>
        </>
      )}
    </div>
  );
}

// ─── FEATURE 5: Budget Suggest ────────────────────────────────────────────────
function BudgetPanel() {
  const [budget, setBudget] = useState(5);
  const [roomType, setRoomType] = useState('studio');
  const [hasAc, setHasAc] = useState(0);
  const [furnished, setFurnished] = useState(0);
  const [numBedrooms, setNumBedrooms] = useState(1);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  async function suggest() {
    setLoading(true);
    setResult(null);
    try {
      const r = await fetch(`${API_BASE}/budget-suggest`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ budgetMillion: budget, roomType, hasAirConditioning: hasAc, furnished, numBedrooms }),
      });
      setResult(await r.json());
    } catch {}
    setLoading(false);
  }

  const tagColors = { '🏆 Nhiều diện tích nhất':'#1a6b3a','⚖️ Cân bằng nhất':'#2f6690','🏙️ Trung tâm nhất':'#7b3fa0' };

  return (
    <div className="panel budget-panel" id="budget">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Tính năng mới</p>
          <h2><Wallet size={20} style={{marginRight:8,verticalAlign:'middle'}}/>Tôi có X triệu thì thuê được gì?</h2>
        </div>
      </div>

      <div className="budget-controls">
        <div className="budget-slider-wrap">
          <div className="budget-amount">{budget} <span>triệu/tháng</span></div>
          <input
            type="range" min="1" max="50" step="0.5" value={budget}
            onChange={e => setBudget(Number(e.target.value))}
            className="budget-slider"
          />
          <div className="budget-range-labels"><span>1 tr</span><span>50 tr</span></div>
        </div>

        <div className="budget-options">
          <label className="mini-label">
            Loại phòng
            <select value={roomType} onChange={e => setRoomType(e.target.value)}>
              <option value="studio">Studio</option>
              <option value="apartment">Căn hộ</option>
              <option value="house">Nhà riêng</option>
            </select>
          </label>
          <label className="mini-label">
            Số phòng ngủ
            <select value={numBedrooms} onChange={e => setNumBedrooms(Number(e.target.value))}>
              <option value={1}>1 PN</option>
              <option value={2}>2 PN</option>
              <option value={3}>3 PN</option>
            </select>
          </label>
          <div className="budget-checks">
            <label className="mini-check">
              <input type="checkbox" checked={hasAc===1} onChange={e => setHasAc(e.target.checked?1:0)} />
              <Wind size={13}/> Điều hòa
            </label>
            <label className="mini-check">
              <input type="checkbox" checked={furnished===1} onChange={e => setFurnished(e.target.checked?1:0)} />
              <Layers size={13}/> Nội thất
            </label>
          </div>
        </div>
      </div>

      <button className="primary-button" onClick={suggest} disabled={loading}>
        {loading ? <Loader2 className="spin" size={18}/> : <Wallet size={18}/>}
        Tìm phòng phù hợp
      </button>

      {result && result.options && (
        <div className="budget-results">
          <p className="budget-result-title">
            <CheckCircle2 size={16}/> Với <strong>{budget} triệu/tháng</strong>, bạn có thể thuê:
          </p>
          <div className="budget-cards">
            {result.options.map((opt, i) => (
              <div className="budget-card" key={opt.district}>
                {opt.valueTag && (
                  <div className="budget-tag" style={{ background: tagColors[opt.valueTag] || '#2f6690' }}>
                    {opt.valueTag}
                  </div>
                )}
                <div className="budget-card-district">{opt.district}</div>
                <div className="budget-card-area">
                  {opt.suggestedAreaMin}–{opt.suggestedAreaMax} m²
                </div>
                <div className="budget-card-price">~{fmt(opt.estimatedPrice)}</div>
                <div className="budget-card-sqm">{(opt.pricePerSqm*1000).toFixed(0)}k / m²</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── MAIN PREDICT FORM ────────────────────────────────────────────────────────
function InsightList({ metadata, prediction }) {
  const price = prediction?.predictedPriceMillion;
  const avg = metadata?.averagePriceMillion;
  const highDistrict = metadata?.districtAverages?.[0];
  return (
    <div className="insight-list">
      <div className="insight"><ShieldCheck size={18}/><p>Giá chỉ là ước lượng theo dữ liệu tin đăng, dùng như khoảng tham khảo trước khi khảo sát thực tế.</p></div>
      <div className="insight"><TrendingUp size={18}/><p>
        {price && avg
          ? `Mức dự đoán ${price >= avg ? 'cao hơn' : 'thấp hơn'} trung bình dataset khoảng ${fmt(Math.abs(price - avg))}.`
          : 'Kết quả sẽ được so với mặt bằng giá sau khi bấm dự đoán.'}
      </p></div>
      <div className="insight"><MapPin size={18}/><p>
        {highDistrict
          ? `Khu vực giá TB cao nhất: ${highDistrict.district} — ${fmt(highDistrict.averagePrice)}.`
          : 'Khu vực giá cao nhất sẽ hiện khi backend có dữ liệu.'}
      </p></div>
    </div>
  );
}

// ─── APP ROOT ─────────────────────────────────────────────────────────────────
function App() {
  const [metadata, setMetadata] = useState(FALLBACK_METADATA);
  const [form, setForm] = useState(DEFAULT_FORM);
  const [prediction, setPrediction] = useState(null);
  const [selectedModel, setSelectedModel] = useState('rf');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('predict'); // 'predict'|'compare'|'budget'|'market'

  useEffect(() => {
    fetch(`${API_BASE}/metadata`)
      .then(r => { if (!r.ok) throw new Error(); return r.json(); })
      .then(data => {
        const m = { ...FALLBACK_METADATA, ...data };
        setMetadata(m);
        if (m.districts?.length) {
          const d = m.districts[0];
          setForm(f => ({ ...f, district: d, distanceToCenter: DISTRICT_DISTANCES[d] || 5 }));
        }
      })
      .catch(() => {});
  }, []);

  const districts = metadata.districts?.length ? metadata.districts : FALLBACK_DISTRICTS;

  function updateField(field, value) {
    if (field === 'district') {
      setForm(f => ({ ...f, district: value, distanceToCenter: DISTRICT_DISTANCES[value] || 5 }));
    } else {
      setForm(f => ({ ...f, [field]: value }));
    }
  }

  async function submitPrediction(e) {
    e.preventDefault();
    setLoading(true); setError(''); setPrediction(null);
    try {
      const r = await fetch(`${API_BASE}/predict`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8' },
        body: JSON.stringify(buildPayload(form)),
      });
      if (!r.ok) throw new Error();
      setPrediction(await r.json());
    } catch { setError('Không thể dự đoán. Kiểm tra backend Spring Boot đã chạy ở cổng 8080.'); }
    setLoading(false);
  }

  const activePred = useMemo(() => {
    if (!prediction) return null;
    return prediction.predictions?.[selectedModel] || {
      predictedPriceMillion: prediction.predictedPriceMillion,
      lowerBoundMillion: prediction.lowerBoundMillion,
      upperBoundMillion: prediction.upperBoundMillion,
      unit: prediction.unit,
      modelName: prediction.modelName,
    };
  }, [prediction, selectedModel]);

  const modelMetrics = useMemo(() => {
    return prediction?.metrics?.length ? prediction.metrics
      : metadata?.metrics?.length ? metadata.metrics : [];
  }, [prediction, metadata]);

  function isRowSelected(name) {
    const l = name.toLowerCase();
    if (selectedModel==='linear' && l.includes('linear')) return true;
    if (selectedModel==='rf' && l.includes('random forest')) return true;
    if (selectedModel==='nn' && l.includes('neural network')) return true;
    if (selectedModel==='xgboost' && l.includes('xgboost')) return true;
    return false;
  }

  const modelOptions = [
    { key:'linear', label:'Linear', Icon:TrendingUp, sub:'Hồi quy tuyến tính' },
    { key:'rf',     label:'RF',     Icon:Triangle,   sub:'Random Forest' },
    { key:'xgboost',label:'XGB',    Icon:BarChart3,  sub:'Gradient Boosting' },
    { key:'nn',     label:'NN',     Icon:Share2,     sub:'Neural Network' },
  ];

  const tabs = [
    { key:'predict', label:'Dự đoán giá', Icon:BarChart3 },
    { key:'compare', label:'So sánh 2 căn', Icon:GitCompare },
    { key:'budget',  label:'Tìm theo budget', Icon:Wallet },
    { key:'market',  label:'Thị trường', Icon:Map },
  ];

  return (
    <main>
      {/* ── topbar ── */}
      <section className="topbar">
        <div className="brand">
          <div className="brand-icon"><Home size={22}/></div>
          <div>
            <strong>Hanoi Rent Estimate</strong>
            <span>Dự đoán giá thuê từ dữ liệu Phongtro123</span>
          </div>
        </div>
        <div className="source"><Sparkles size={16}/><span>phongtro123.com · Hà Nội</span></div>
      </section>

      {/* ── hero ── */}
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">House Price Prediction</p>
          <h1>Dự đoán giá thuê phòng tại Hà Nội</h1>
          <p>Tham khảo giá thuê theo diện tích, khu vực, tiện nghi. So sánh 2 căn, tìm phòng theo ngân sách, xem bản đồ nhiệt và phân tích feature.</p>
          <div className="hero-actions">
            <a href="#main-tabs">Bắt đầu ngay</a>
            <a href="#market" className="secondary-link">Xem thị trường</a>
          </div>
        </div>
        <div className="market-card">
          <div><span>Giá trung bình</span><strong>{fmt(metadata.averagePriceMillion)}</strong></div>
          <div><span>Khoảng giá</span><strong>{fmt(metadata.minPriceMillion)} – {fmt(metadata.maxPriceMillion)}</strong></div>
          <div><span>Dữ liệu training</span><strong>{fmtNum(metadata.trainingRows)} tin đăng</strong></div>
        </div>
      </section>

      {/* ── stats ── */}
      <section className="stats-row">
        <Stat icon={<Database size={18}/>} label="Dòng training" value={fmtNum(metadata.trainingRows)} hint="Sau cleaning & outlier"/>
        <Stat icon={<Activity size={18}/>} label="Số feature" value={fmtNum(metadata.featureCount)} hint="Sau one-hot encoding"/>
        <Stat icon={<BarChart3 size={18}/>} label="Giá trung bình" value={fmt(metadata.averagePriceMillion)} hint="Triệu VND/tháng"/>
        <Stat icon={<Ruler size={18}/>} label="Diện tích TB" value={metadata.averageArea ? `${metadata.averageArea.toFixed(1)} m²` : '--'} hint="Theo dataset"/>
      </section>

      {/* ── main tabs ── */}
      <section id="main-tabs">
        <div className="tab-bar">
          {tabs.map(({ key, label, Icon }) => (
            <button key={key} className={`tab-btn ${activeTab===key?'active':''}`}
              onClick={() => setActiveTab(key)}>
              <Icon size={16}/>{label}
            </button>
          ))}
        </div>

        {/* TAB: PREDICT */}
        {activeTab === 'predict' && (
          <div className="workspace">
            <form className="panel form-panel" onSubmit={submitPrediction}>
              <div className="panel-heading">
                <div><p className="eyebrow">Thông tin đầu vào</p><h2>Cấu hình căn phòng</h2></div>
                <Building2 size={22}/>
              </div>

              <label>
                <span><Ruler size={16}/> Diện tích</span>
                <div className="input-with-unit">
                  <input type="number" min="5" max="500" step="1" value={form.area}
                    onChange={e => updateField('area', e.target.value)}/>
                  <em>m²</em>
                </div>
              </label>

              <label>
                <span><MapPin size={16}/> Khu vực</span>
                <select value={form.district} onChange={e => updateField('district', e.target.value)}>
                  {districts.map(d => <option key={d} value={d}>{d}</option>)}
                </select>
              </label>

              <label>
                <span><Building2 size={16}/> Loại phòng</span>
                <select value={form.roomType} onChange={e => updateField('roomType', e.target.value)}>
                  <option value="studio">Phòng trọ Studio</option>
                  <option value="apartment">Căn hộ chung cư</option>
                  <option value="house">Nhà nguyên căn / riêng</option>
                </select>
              </label>

              <div className="two-col">
                <label>
                  <span><Bed size={16}/> Phòng ngủ</span>
                  <input type="number" min="1" max="20" value={form.numBedrooms}
                    onChange={e => updateField('numBedrooms', e.target.value)}/>
                </label>
                <label>
                  <span><Bath size={16}/> Phòng tắm</span>
                  <input type="number" min="1" max="20" value={form.numBathrooms}
                    onChange={e => updateField('numBathrooms', e.target.value)}/>
                </label>
              </div>

              <div className="two-col">
                <label>
                  <span>Tầng</span>
                  <input type="number" min="1" max="100" value={form.floor}
                    onChange={e => updateField('floor', e.target.value)}/>
                </label>
                <label>
                  <span>Khoảng cách TT</span>
                  <div className="input-with-unit">
                    <input type="number" min="0" max="100" step="0.1" value={form.distanceToCenter}
                      onChange={e => updateField('distanceToCenter', e.target.value)}/>
                    <em>km</em>
                  </div>
                </label>
              </div>

              <div className="checkbox-row">
                <label className="check-label">
                  <input type="checkbox" checked={form.hasAirConditioning===1}
                    onChange={e => updateField('hasAirConditioning', e.target.checked?1:0)}/>
                  <Wind size={15}/> Có điều hòa
                </label>
                <label className="check-label">
                  <input type="checkbox" checked={form.furnished===1}
                    onChange={e => updateField('furnished', e.target.checked?1:0)}/>
                  <Layers size={15}/> Có nội thất
                </label>
              </div>

              <div className="three-col">
                {[['day','Ngày',1,31],['month','Tháng',1,12]].map(([f,l,mn,mx]) => (
                  <label key={f}>
                    <span>{l}</span>
                    <input type="number" min={mn} max={mx} value={form[f]}
                      onChange={e => updateField(f, e.target.value)}/>
                  </label>
                ))}
                <label>
                  <span>Năm</span>
                  <input type="number" min="2015" max="2030" value={form.year}
                    onChange={e => updateField('year', e.target.value)}/>
                </label>
              </div>

              <div className="model-selector-container">
                <label><span><Database size={16}/> Model dự đoán</span></label>
                <div className="model-selector-grid">
                  {modelOptions.map(({ key, label, Icon, sub }) => (
                    <button key={key} type="button"
                      className={`model-card ${selectedModel===key?'selected':''}`}
                      onClick={() => setSelectedModel(key)}>
                      <Icon size={18}/><strong>{label}</strong><span>{sub}</span>
                    </button>
                  ))}
                </div>
              </div>

              <button className="primary-button" type="submit" disabled={loading}>
                {loading ? <Loader2 className="spin" size={18}/> : <BarChart3 size={18}/>}
                Dự đoán giá thuê
              </button>
            </form>

            <aside className="panel result-panel">
              <div className="panel-heading">
                <div><p className="eyebrow">Kết quả model</p><h2>Ước lượng giá thuê</h2></div>
                <TrendingUp size={22}/>
              </div>

              {error && <div className="error">{error}</div>}

              <div className="estimate">
                <span>{activePred ? activePred.modelName : 'Giá dự đoán'}</span>
                <strong>{activePred ? fmt(activePred.predictedPriceMillion) : '--'}</strong>
                <p>{activePred ? activePred.unit : 'triệu VND/tháng'}</p>
              </div>

              <div className="range">
                <div><span>Cận dưới</span><strong>{activePred ? fmt(activePred.lowerBoundMillion) : '--'}</strong></div>
                <div><span>Cận trên</span><strong>{activePred ? fmt(activePred.upperBoundMillion) : '--'}</strong></div>
              </div>

              {/* giá/m² nhanh */}
              {activePred && (
                <div className="price-per-sqm-badge">
                  <Info size={14}/>
                  Giá/m²: <strong>~{((activePred.predictedPriceMillion / form.area)*1000).toFixed(0)}k VND/m²</strong>
                </div>
              )}

              {modelMetrics.length > 0 && (
                <div className="comparison-container">
                  <h3 className="comparison-title">So sánh 4 model (Test set)</h3>
                  <table className="comparison-table">
                    <thead><tr><th>Model</th><th>MAE</th><th>RMSE</th><th>R²</th></tr></thead>
                    <tbody>
                      {modelMetrics.map(m => (
                        <tr key={m.model} className={isRowSelected(m.model)?'selected-row':''}>
                          <td>{m.model}</td>
                          <td>{m.mae.toFixed(3)}</td>
                          <td>{m.rmse.toFixed(3)}</td>
                          <td>{m.r2.toFixed(3)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              <InsightList metadata={metadata} prediction={activePred}/>
            </aside>
          </div>
        )}

        {/* TAB: COMPARE */}
        {activeTab === 'compare' && (
          <ComparePanel districts={districts} myForm={form} myPrediction={prediction}/>
        )}

        {/* TAB: BUDGET */}
        {activeTab === 'budget' && (
          <BudgetPanel/>
        )}

        {/* TAB: MARKET */}
        {activeTab === 'market' && (
          <div id="market">
            <div className="section-title">
              <div><p className="eyebrow">Market Dashboard</p><h2>Góc nhìn dữ liệu thị trường</h2></div>
              <span>Từ houseprice_dataset.csv</span>
            </div>

            {/* Heat Map + bar chart row */}
            <div className="market-top-row">
              <article className="panel chart-panel">
                <div className="chart-heading">
                  <h3><Map size={16} style={{marginRight:6,verticalAlign:'middle'}}/>Bản đồ nhiệt giá theo quận</h3>
                  <span>Màu đậm = giá cao hơn</span>
                </div>
                <HeatMap districtAverages={metadata.districtAverages}/>
              </article>

              <article className="panel chart-panel">
                <div className="chart-heading">
                  <h3>Giá trung bình theo khu vực</h3>
                  <span>Top khu vực có giá TB cao</span>
                </div>
                {metadata.districtAverages.length
                  ? <BarChart data={metadata.districtAverages}/>
                  : <div className="empty-chart">Chưa có dữ liệu</div>}
              </article>
            </div>

            <div className="chart-grid">
              <article className="panel chart-panel">
                <div className="chart-heading">
                  <h3>Phân phối khoảng giá</h3>
                  <span>Số tin theo triệu VND/tháng</span>
                </div>
                {metadata.priceBuckets.length
                  ? <Histogram data={metadata.priceBuckets}/>
                  : <div className="empty-chart">Chưa có dữ liệu</div>}
              </article>

              <article className="panel chart-panel">
                <div className="chart-heading">
                  <h3>Diện tích và giá thuê</h3>
                  <span>Mỗi điểm là một tin đăng</span>
                </div>
                {metadata.areaPricePoints.length
                  ? <ScatterPlot data={metadata.areaPricePoints}/>
                  : <div className="empty-chart">Chưa có dữ liệu</div>}
              </article>
            </div>

            {/* Feature Importance collapsible */}
            <div style={{marginTop:20}}>
              <FeatureImportancePanel/>
            </div>
          </div>
        )}
      </section>
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App/>);
