import React, {useEffect, useMemo, useState} from 'react';
import {useDateStyles} from "../../utils/date-styles";

// interpolate from green (120°) to red (0°) in HSL space
function colorFor(t) {
    const clamped = Math.max(0, Math.min(1, t));
    const hue = 120 - clamped * 120;
    return `hsl(${hue.toFixed(1)},100%,50%)`;
}

function isInTimeRange(idx, [from, to]) {
    return from <= to
        ? idx >= from && idx < to
        : idx >= from || idx < to;
}

const CarbonIntensityChart = ({jobState}) => {
    const style = useDateStyles();
    const useUTC = style === 'iso8601Style';

    // raw forecast value
    const [intensityData, setIntensityData] = useState(null);

    useEffect(() => {
        fetch('/api/metadata/carbon-intensity-forecast')
            .then(r => r.json())
            .then(payload => {
                const raw = payload[0].value;
                const value = typeof raw === 'string' ? JSON.parse(raw) : raw;
                setIntensityData(value);
            });
    }, [useUTC]);

    const intervalSeconds = useMemo(
        () => intensityData?.forecastInterval ?? 3600,
        [intensityData]
    );

    const slotLabels = useMemo(() => {
        const count = 24 * 3600 / intervalSeconds;
        return Array.from({length: count}, (_, i) => {
            const total = i * intervalSeconds;
            const h = Math.floor(total / 3600);
            const m = (total % 3600) / 60;
            return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
        });
    }, [intervalSeconds]);

    const windowRange = useMemo(() => {
        if (!intensityData) return [0, 0];
        const getter = date => useUTC
            ? [date.getUTCHours(), date.getUTCMinutes(), date.getUTCSeconds()]
            : [date.getHours(), date.getMinutes(), date.getSeconds()];
        const [fH, fM, fS] = getter(new Date(jobState.from));
        const [tH, tM, tS] = getter(new Date(jobState.to));
        const fSec = fH * 3600 + fM * 60 + fS;
        const tSec = tH * 3600 + tM * 60 + tS;
        return [
            Math.floor(fSec / intervalSeconds),
            Math.floor(tSec / intervalSeconds)
        ];
    }, [jobState.from, jobState.to, intervalSeconds, useUTC, intensityData]);

    const normalized = useMemo(() => {
        if (!intensityData) return null;
        const raw = {};
        intensityData.intensityForecast.forEach(({periodStartAt, rank}) => {
            const d = new Date(periodStartAt);
            const h = useUTC ? d.getUTCHours() : d.getHours();
            const m = useUTC ? d.getUTCMinutes() : d.getMinutes();
            const label = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
            raw[label] = Math.min(rank, raw[label] ?? Infinity);
        });
        const filled = {};
        slotLabels.forEach(l => {
            filled[l] = raw[l] ?? null;
        });
        const vals = Object.values(filled).filter(v => v !== null);
        const rMin = Math.min(...vals), rMax = Math.max(...vals);
        return Object.fromEntries(
            Object.entries(filled).map(([slot, rank]) => {
                if (rank === null) return [slot, {rank: null, t: null, color: '#ccc'}];
                const t = (rank - rMin) / (rMax - rMin);
                return [slot, {rank, t, color: colorFor(t)}];
            })
        );
    }, [intensityData, slotLabels, useUTC]);

    const idealSlot = useMemo(() => {
        if (!normalized) return null;
        return Object.entries(normalized).reduce(
            (best, [slot, {rank}]) => {
                if (rank === null) return best;
                const idx = slotLabels.indexOf(slot);
                if (isInTimeRange(idx, windowRange) && rank < best.rank) {
                    return {slot, rank};
                }
                return best;
            },
            {slot: null, rank: Infinity}
        ).slot;
    }, [normalized, windowRange, slotLabels]);

    if (!normalized) return null;

    return (
        <div style={{width: '100%', marginTop: '32px'}}>
            <div style={{
                display: 'grid',
                gridTemplateColumns: `repeat(${slotLabels.length}, 1fr)`,
                gridTemplateRows: '2px 16px 16px',
                gridAutoFlow: 'column',
                rowGap: '4px',
                marginBottom: '8px',
            }}>
                {slotLabels.map((slot, idx) => {
                    const {rank, color} = normalized[slot];
                    const inWindow = isInTimeRange(idx, windowRange);
                    const isBest = slot === idealSlot;
                    return (
                        <React.Fragment key={slot}>
                            <div style={{background: inWindow ? 'transparent' : '#E53935'}}/>
                            <div style={{
                                background: color,
                                opacity: inWindow ? 1 : 0.6,
                                transform: isBest ? 'scaleY(1.2)' : 'scaleY(1)',
                                transformOrigin: 'center',
                                boxSizing: 'border-box',
                                width: '100%',
                                height: '100%',
                                position: 'relative',
                            }}
                                 title={`Time ${slot} — ${rank !== null
                                     ? `Intensity: ${rank} (0 = best)`
                                     : 'no data'}`}
                            >
                                {isBest && (
                                    <div style={{
                                        position: 'absolute', top: '-14px', left: '50%', transform: 'translateX(-50%)',
                                        width: 0, height: 0,
                                        borderLeft: '6px solid transparent',
                                        borderRight: '6px solid transparent',
                                        borderTop: '12px solid #fbe200',
                                    }}/>
                                )}
                            </div>
                            <div style={{textAlign: 'center', fontSize: '8px', color: '#333'}}>
                                {slot.endsWith(':00') ? slot : ''}
                            </div>
                        </React.Fragment>
                    );
                })}
            </div>
            <div style={{display: 'flex', gap: '16px', fontSize: '12px'}}>
                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    {intensityData.dataProviderUrl
                        ? <span>Dataprovider: <a href={intensityData.dataProviderUrl} target="_blank"
                                                 rel="noopener">{intensityData.dataProvider} ({intensityData.displayName})</a></span>
                        : <span>Dataprovider: {intensityData.dataProvider} ({intensityData.displayName})</span>
                    }
                </div>
                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    <div style={{
                        width: 0,
                        height: 0,
                        borderLeft: '6px solid transparent',
                        borderRight: '6px solid transparent',
                        borderTop: '12px solid #fbe200'
                    }}/>
                    <span>Optimal execution window ({idealSlot}{useUTC ? ' UTC' : ''})</span>
                </div>
                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    <div style={{width: '16px', height: '2px', background: '#E53935'}}/>
                    <span>Outside execution window</span>
                </div>
            </div>
        </div>
    );
};

export default CarbonIntensityChart;