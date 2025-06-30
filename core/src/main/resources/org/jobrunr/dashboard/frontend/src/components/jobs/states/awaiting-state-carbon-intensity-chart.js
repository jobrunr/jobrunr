import React, {useEffect, useMemo, useState} from 'react'

const hours = Array.from({length: 24}, (_, i) =>
    String(i).padStart(2, '0'),
)
const colors = [
    "#00FF00", "#0BF400", "#16E900", "#21DE00", "#2CD300", "#37C800",
    "#43BC00", "#4EB100", "#59A600", "#649B00", "#6F9000", "#7A8500",
    "#857A00", "#906F00", "#9B6400", "#A65900", "#B14E00", "#BC4300",
    "#C83700", "#D32C00", "#DE2100", "#E91600", "#F30C00", "#FF0000"
]

const getNormalizedHourlyRanks = (forecast) => {
    // 1) bucket all points into hours → pick min rank per hour
    const hourlyRanks = forecast.reduce((acc, {periodStartAt, rank}) => {
        const hour = new Date(periodStartAt).getUTCHours()
        const key = hour.toString().padStart(2, '0')
        acc[key] = Math.min(rank, acc[key] ?? Infinity);
        return acc;
    }, {});

    const dailyHoursRanks = hours.reduce((acc, key) => {
        acc[key] = hourlyRanks[key] ? hourlyRanks[key] : null;
        return acc;
    }, {})

    // 2) normalize rawRanks[h] from [rawMin..rawMax] → [0..colors.length-1]
    const vals = Object.values(dailyHoursRanks).filter(v => v !== null);
    const rankMin = Math.min(...vals);
    const rankMax = Math.max(...vals);
    const N = colors.length - 1;
    return Object.keys(dailyHoursRanks).reduce((acc, hour) => {
        const rank = dailyHoursRanks[hour];
        if (rank === null) {
            acc[hour] = null;
        } else {
            const t = (rank - rankMin) / (rankMax - rankMin)
            acc[hour] = Math.round(t * N)
        }
        return acc;
    }, {});
}

const isInTimeRange = (hour, [from, to]) => {
    return from <= to
        ? hour >= from && hour < to
        : hour >= from || hour < to;
}


const getIdealMoment = (range, ranks) => {
    if (!ranks) return;
    return Object.keys(ranks).reduce((acc, key) => {
        const rank = ranks[key];
        if (rank === null) return acc;
        const hour = parseInt(key, 10);
        if (isInTimeRange(hour, range) && rank < acc.minimumRank) {
            return {idealMoment: key, minimumRank: rank};
        }
        return acc;
    }, {idealMoment: null, minimumRank: Infinity}).idealMoment;
}

const CarbonIntensityChart = ({jobState}) => {
    const [ranks, setRanks] = useState(null)   // { "00": 5, "01": 3, ... }

    const windowRange = useMemo(() =>
            // TODO these from and to may be over different days
            ([new Date(jobState.from).getUTCHours(), new Date(jobState.to).getUTCHours()]),
        [jobState]);

    const bestHour = useMemo(() => getIdealMoment(windowRange, ranks), [windowRange, ranks]);

    useEffect(() => {
        fetch('/api/metadata/carbon-intensity-forecast')
            .then(r => r.json())
            .then(payload => {
                const rawValue = payload[0].value;
                const value = typeof rawValue === 'string' ? JSON.parse(rawValue) : rawValue
                const normalizedHourlyRanks = getNormalizedHourlyRanks(value.intensityForecast);
                setRanks(normalizedHourlyRanks);
            })
    }, []);

    if (!ranks) return;

    return (
        <div style={{width: '100%', marginTop: '32px'}}>
            <div
                style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(24, 1fr)',
                    gridTemplateRows: '2px 16px 16px',
                    gridAutoFlow: 'column',    // ← add this
                    rowGap: '4px',
                    marginBottom: '8px'
                }}
            >
                {hours.map((h, idx) => {
                    const rank = ranks[h];
                    const inWindow = isInTimeRange(idx, windowRange);
                    const isBest = h === bestHour;
                    const color = colors[rank];
                    return (
                        <React.Fragment key={h}>
                            <div style={{background: inWindow ? 'transparent' : '#E53935'}}/>
                            <div style={{
                                position: 'relative',
                                background: color,
                                opacity: inWindow ? 1 : 0.6,
                                transform: isBest ? 'scaleY(1.2)' : 'scaleY(1)',
                                transformOrigin: 'center',
                                boxSizing: 'border-box',
                                width: '100%',
                                height: '100%'
                            }}
                                 title={`Hour ${h}: rank ${rank}${inWindow ? '' : ' (unavailable)'}`}
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
                                            borderTop: '12px solid #fbe200'
                                        }}
                                    />
                                )}
                            </div>
                            <div style={{textAlign: 'center', fontSize: '10px', color: '#333'}}>{h}</div>
                        </React.Fragment>
                    );
                })}
            </div>

            <div style={{display: 'flex', gap: '16px', fontSize: '12px'}}>
                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    <div
                        style={{
                            width: 0,
                            height: 0,
                            borderLeft: '6px solid transparent',
                            borderRight: '6px solid transparent',
                            borderTop: '12px solid #fbe200',
                        }}
                    />
                    <span>Best hour (rank {ranks[bestHour]})</span>
                </div>

                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    <div style={{width: '16px', height: '2px', background: '#E53935'}}/>
                    <span>Outside execution window</span>
                </div>
            </div>
        </div>
    );
}

export default CarbonIntensityChart;