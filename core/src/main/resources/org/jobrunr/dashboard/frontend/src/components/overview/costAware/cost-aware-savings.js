import {useEffect, useState} from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import CostAwareDailySavingsCard from "./daily-savings-card.js";
import CostAwareMonthlySavingsCard from "./monthly-savings-card.js";
import CostAwareYearlySavingsCard from "./yearly-savings-card.js";

const CostAwareSavings = () => {
    const [totalSavings, setTotalSavings] = useState(null);
    const [dailySavings, setDailySavings] = useState(new Map());
    const [monthlySavings, setMonthlySavings] = useState(new Map());
    const [yearlySavings, setYearlySavings] = useState(new Map());
    const [notFound, setNotFound] = useState(false);


    useEffect(() => {
        fetch(`/api/metadata/total-savings/cluster?format=jsonValue`)
            .then(async r => {
                if (r.status === 404) {
                    setNotFound(true);
                } else {
                    const payload = await r.json();
                    setTotalSavings(payload);
                }
            });
    }, []);

    useEffect(() => {
        if (totalSavings) {
            const dailySavingsMap = new Map();
            Object.keys(totalSavings.dailySavings).forEach(function (key) {
                if (key !== "@class") {
                    dailySavingsMap.set(key, totalSavings.dailySavings[key]);
                }
            })
            setDailySavings(dailySavingsMap);

            const monthlySavingsMap = new Map();
            Object.keys(totalSavings.monthlySavings).forEach(function (key) {
                if (key !== "@class") {
                    monthlySavingsMap.set(key, totalSavings.monthlySavings[key]);
                }
            })
            setMonthlySavings(monthlySavingsMap);

            const yearlySavingsMap = new Map();
            Object.keys(totalSavings.yearlySavings).forEach(function (key) {
                if (key !== "@class") {
                    yearlySavingsMap.set(key, totalSavings.yearlySavings[key]);
                }
            })
            setYearlySavings(yearlySavingsMap);
        }
    }, [totalSavings])

    const isAtLeastOneSavingPresent = () => {
        return dailySavings.size > 0 || monthlySavings.size > 0 || yearlySavings.size > 0;
    }

    return (
        <>
            {!notFound && isAtLeastOneSavingPresent() && <div className="row">
                <Box my={3}>
                    <Typography id="total-savings" variant="h5">Cost Aware Spot Savings</Typography>
                </Box>
                <div style={{display: "flex", flexWrap: "wrap", gap: "16px"}}>
                    {dailySavings && dailySavings.size > 0 &&
                        <CostAwareDailySavingsCard dailySavings={dailySavings}/>
                    }
                    {monthlySavings && monthlySavings.size > 0 &&
                        <CostAwareMonthlySavingsCard monthlySavings={monthlySavings}/>
                    }
                    {yearlySavings && yearlySavings.size > 0 &&
                        <CostAwareYearlySavingsCard yearlySavings={yearlySavings}/>
                    }
                </div>
            </div>}
        </>
    )
}

export default CostAwareSavings;