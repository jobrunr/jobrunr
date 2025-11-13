import React, {useEffect, useMemo, useState} from 'react';
import {useDateStyles} from "../../../hooks/useDateStyles.js";
import {extractDateFromISOString, extractTimeFromDate, formatTime, SwitchableTimeFormatter} from "../../utils/time-ago";

// interpolate from green (120°) to red (0°) in HSL space
function colorFor(t) {
    const clamped = Math.max(0, Math.min(1, t));
    const hue = 120 - clamped * 120;
    return `hsl(${hue.toFixed(2)},100%,50%)`;
}

function isInTimeRange(idx, [from, to]) {
    return from <= to
        ? idx >= from && idx < to
        : idx >= from || idx < to;
}

const CarbonIntensityChart = ({job, jobState}) => {
    const style = useDateStyles();
    const useUTC = style === 'iso8601Style';

    const scheduledState = useMemo(
        () => job.jobHistory.find(js => js.state === 'SCHEDULED'),
        [job.jobHistory]
    );

    const [intensityData, setIntensityData] = useState(null);
    const [notFound, setNotFound] = useState(false);

    useEffect(() => {
        if (!scheduledState) return;
        const ownerDate = extractDateFromISOString(scheduledState.scheduledAt, useUTC);
        fetch(`/api/metadata/carbon-intensity-forecast/${ownerDate}?format=jsonValue`)
            .then(async r => {
                if (r.status === 404 && new Date(ownerDate) < new Date()) {
                    setNotFound(true);
                } else {
                    const payload = await r.json();
                    setIntensityData(payload);
                }
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
            return formatTime(h, m);
        });
    }, [intervalSeconds]);

    const {windowRange, isWindowValid, visualizeDate} = useMemo(() => {
        if (!intensityData || !scheduledState) {
            return {windowRange: [0, 0], isWindowValid: false, visualizeDate: null};
        }

        const dateList = intensityData.intensityForecast
            .map(({periodStartAt}) => extractDateFromISOString(periodStartAt, useUTC))
            .filter((v, i, a) => a.indexOf(v) === i);

        const fromDate = extractDateFromISOString(jobState.from, useUTC);
        const toDate = extractDateFromISOString(jobState.to, useUTC);

        const visualizeDate = dateList.includes(fromDate) ? fromDate : dateList[0];

        const fromDayIndex = dateList.indexOf(fromDate);
        const toDayIndex = dateList.indexOf(toDate);
        const isValid = fromDayIndex > -1 && toDayIndex > -1;
        if (!isValid) {
            return {windowRange: [0, 0], isWindowValid: false, visualizeDate};
        }

        const [fH, fM, fS] = extractTimeFromDate(new Date(jobState.from), useUTC);
        const [tH, tM, tS] = extractTimeFromDate(new Date(jobState.to), useUTC);
        const fSec = fH * 3600 + fM * 60 + fS;
        const tSec = tH * 3600 + tM * 60 + tS;

        const startIndex = Math.floor(fSec / intervalSeconds);
        const endIndex = Math.floor(tSec / intervalSeconds);

        return {
            windowRange: [startIndex, endIndex],
            isWindowValid: true,
            visualizeDate
        };
    }, [jobState.from, jobState.to, intervalSeconds, useUTC, intensityData, scheduledState]);

    const normalized = useMemo(() => {
        if (!intensityData || !visualizeDate) return null;

        const filtered = intensityData.intensityForecast.filter(
            ({periodStartAt}) =>
                extractDateFromISOString(periodStartAt, useUTC) === visualizeDate
        );

        const raw = {};
        filtered.forEach(({periodStartAt, rank}) => {
            const [h, m] = extractTimeFromDate(new Date(periodStartAt), useUTC);
            const label = formatTime(h, m);
            raw[label] = Math.min(rank, raw[label] ?? Infinity);
        });

        const filled = {};
        slotLabels.forEach(l => (filled[l] = raw[l] ?? null));

        const vals = Object.values(filled).filter(v => v !== null);
        const rMin = Math.min(...vals), rMax = Math.max(...vals);

        return Object.fromEntries(
            Object.entries(filled).map(([slot, rank]) => {
                if (rank === null) return [slot, {rank: null, t: null, color: '#ccc'}];
                const t = (rank - rMin) / (rMax - rMin);
                return [slot, {rank, t, color: colorFor(t)}];
            })
        );
    }, [intensityData, slotLabels, useUTC, visualizeDate]);

    const idealSlot = useMemo(() => {
        if (!scheduledState || !slotLabels.length) return null;
        const date = new Date(scheduledState.scheduledAt);
        const [h, m, s] = extractTimeFromDate(date, useUTC);
        const idx = Math.floor((h * 3600 + m * 60 + s) / intervalSeconds);
        return slotLabels[idx];
    }, [scheduledState, slotLabels, intervalSeconds, useUTC]);

    const lastDateAvailable = useMemo(() => {
        if (!intensityData) return null;
        const times = intensityData.intensityForecast.map(
            ({periodEndAt}) => new Date(periodEndAt).getTime()
        );
        return new Date(Math.max(...times));
    }, [intensityData]);

    if (notFound) {
        return (
            <div style={{paddingTop: '12px'}}>
                No forecast available anymore for this date - it has been purged.
            </div>
        );
    } else if (!normalized) {
        return null;
    }

    if (!scheduledState) return <div/>;

    return (
        <div style={{width: '100%', marginTop: '32px'}} className={"carbon-intensity-chart"}>
            <div
                style={{
                    display: 'grid',
                    gridTemplateColumns: `repeat(${slotLabels.length}, minmax(0, 1fr))`,
                    gridTemplateRows: '2px 16px 16px',
                    gridAutoFlow: 'column',
                    rowGap: '4px',
                    marginBottom: '8px',
                }}
            >
                {slotLabels.map((slot, idx) => {
                    const {rank, color} = normalized[slot];
                    const inWindow = isWindowValid && isInTimeRange(idx, windowRange);
                    const isBest = slot === idealSlot;

                    return (
                        <React.Fragment key={slot}>
                            <div style={{background: inWindow ? 'transparent' : '#E53935'}}/>
                            <div
                                className={isBest ? "carbon-intensity-chart-block-best" : "carbon-intensity-chart-block"}
                                style={{
                                    background: color,
                                    opacity: inWindow ? 1 : 0.6,
                                    transform: isBest ? 'scaleY(1.2)' : 'scaleY(1)',
                                    transformOrigin: 'center',
                                    boxSizing: 'border-box',
                                    width: '100%',
                                    height: '100%',
                                    position: 'relative',
                                }}
                                title={isBest ?
                                    `${slot}: Optimal execution window — Intensity: ${rank}` :
                                    `Time ${slot} — ${rank !== null ? `Intensity: ${rank} (0 = best)` : 'no data'}`}
                            >
                                {isBest && (
                                    <div
                                        style={{
                                            position: 'absolute',
                                            top: '-14px',
                                            left: '50%',
                                            transform: 'translateX(-50%)',
                                            width: 0,
                                            height: 0,
                                            borderLeft: '6px solid transparent',
                                            borderRight: '6px solid transparent',
                                            borderTop: '12px solid #fbe200',
                                        }}
                                    />
                                )}
                            </div>
                            <div style={{
                                textAlign: slot === '00:00' ? 'left' : 'center',
                                justifySelf: slot === '00:00' ? 'start' : 'center',
                                fontSize: '12px',
                                color: '#333'
                            }}>
                                {slot.endsWith(':00') ? slot : ''}
                            </div>
                        </React.Fragment>
                    );
                })}
            </div>

            <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                marginTop: '4px',
                color: '#333'
            }}>
                <div style={{fontSize: '12px', lineHeight: 1, alignSelf: 'start', marginTop: '4px'}}>▲&nbsp;
                    <span>{visualizeDate}</span>
                </div>
            </div>

            <div style={{display: 'flex', gap: '16px', fontSize: '12px', marginTop: '16px'}}>
                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    Forecast until: <SwitchableTimeFormatter date={lastDateAvailable}/>
                </div>
                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    {intensityData.dataProviderUrl
                        ? <span>Dataprovider: <a
                            href={intensityData.dataProviderUrl}
                            target="_blank" rel="noopener">{intensityData.dataProvider} ({intensityData.displayName})</a></span>
                        : <span>Dataprovider: {intensityData.dataProvider} ({intensityData.displayName})</span>
                    }
                </div>
                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    {idealSlot
                        ? <>
                            <div style={{
                                width: 0,
                                height: 0,
                                borderLeft: '6px solid transparent',
                                borderRight: '6px solid transparent',
                                borderTop: '12px solid #fbe200'
                            }}/>
                            <span>Optimal execution window ({idealSlot}{useUTC ? ' UTC' : ''})</span>
                        </>
                        : <span>Waiting for data to determine best execution moment</span>
                    }
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