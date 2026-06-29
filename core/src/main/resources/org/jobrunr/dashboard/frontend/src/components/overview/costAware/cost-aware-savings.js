import {useEffect, useState} from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import CostAwareSavingsThisMonthCard from "./savings-this-month-card.js";
import CostAwareAllTimeSavingsCard from "./all-time-savings-card.js";
import CostAwareSavingsTodayCard from "./savings-today-card.js";
import CostAwareSavingsPastWeekCard from "./savings-past-week-card.js";
import CostAwareSavingsThisYearCard from "./savings-this-year-card.js";
import IconButton from "@mui/material/IconButton";
import {History} from "@mui/icons-material";
import {Dialog, Tab, Tabs} from "@mui/material";
import MuiDialogTitle from "@mui/material/DialogTitle";
import MuiDialogContent from "@mui/material/DialogContent";
import CostAwareDailySavingsTab from "./daily-savings-tab.js";
import CostAwareMonthlySavingsTab from "./monthly-savings-tab.js";
import CostAwareYearlySavingsTab from "./yearly-savings-tab.js";

const CostAwareSavings = () => {
    const [dailySavings, setDailySavings] = useState(new Map());
    const [monthlySavings, setMonthlySavings] = useState(new Map());
    const [yearlySavings, setYearlySavings] = useState(new Map());
    const [notFound, setNotFound] = useState(false);
    const [open, setOpen] = useState(false);
    const [selectedTab, setSelectedTab] = useState(0);


    useEffect(() => {
        fetch(`/api/metadata/total-savings/cluster?format=jsonValue`)
            .then(async r => {
                if (r.status === 404) {
                    setNotFound(true);
                } else {
                    const totalSavings = await r.json();

                    const dailySavingsMap = new Map();
                    Object.keys(totalSavings.dailySavings).forEach(function (key) {
                        dailySavingsMap.set(key, totalSavings.dailySavings[key]);
                    })
                    setDailySavings(dailySavingsMap);

                    const monthlySavingsMap = new Map();
                    Object.keys(totalSavings.monthlySavings).forEach(function (key) {
                        monthlySavingsMap.set(key, totalSavings.monthlySavings[key]);
                    })
                    setMonthlySavings(monthlySavingsMap);

                    const yearlySavingsMap = new Map();
                    Object.keys(totalSavings.yearlySavings).forEach(function (key) {
                        yearlySavingsMap.set(key, totalSavings.yearlySavings[key]);
                    })
                    setYearlySavings(yearlySavingsMap);
                }
            });
    }, []);

    const isAtLeastOneSavingPresent = () => {
        return dailySavings.size > 0 || monthlySavings.size > 0 || yearlySavings.size > 0;
    }

    const handleOpen = () => {
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
    };

    const handleChange = (_, selectedTab) => {
        setSelectedTab(selectedTab);
    }

    return (
        <>
            {!notFound && isAtLeastOneSavingPresent() && <div className="row">
                <Box my={3} display={"flex"}>
                    <Typography id="total-savings" variant="h5">Cost Aware Spot Savings</Typography>
                    <IconButton onClick={handleOpen}>
                        <History/>
                    </IconButton>
                </Box>
                <div style={{display: "flex", flexWrap: "wrap", gap: "16px"}}>
                    {dailySavings && dailySavings.size > 0 &&
                        <>
                            <CostAwareSavingsTodayCard dailySavings={dailySavings}/>
                            <CostAwareSavingsPastWeekCard dailySavings={dailySavings}/>
                            <CostAwareSavingsThisMonthCard dailySavings={dailySavings}/>
                        </>
                    }
                    {monthlySavings && monthlySavings.size > 0 &&
                        <CostAwareSavingsThisYearCard dailySavings={dailySavings} monthlySavings={monthlySavings}/>
                    }
                    <CostAwareAllTimeSavingsCard dailySavings={dailySavings} monthlySavings={monthlySavings} yearlySavings={yearlySavings}/>
                </div>
            </div>}

            <Dialog open={open} onClose={handleClose} fullWidth={true} maxWidth={"md"}>
                <MuiDialogTitle id="customized-dialog-title" onClose={handleClose}>
                    Cost Aware Savings
                </MuiDialogTitle>
                <MuiDialogContent dividers>
                    <Box display="flex" flexDirection="column" gap={2}>
                        <Tabs
                            value={selectedTab}
                            onChange={handleChange}
                        >
                            <Tab label="Daily Savings" id={"daily-savings-tab"} aria-controls={"daily-savings-tabpanel"}></Tab>
                            <Tab label={"Monthly Savings"} id={"monthly-savings-tab"} aria-controls={"monthly-savings-tabpanel"}></Tab>
                            <Tab label={"Yearly Savings"} id={"yearly-savings-tab"} aria-controls={"yearly-savings-tabpanel"}></Tab>
                        </Tabs>
                        <Box id={"daily-savings-tabpanel"} aria-labelledby={"daily-savings-tab"} hidden={selectedTab !== 0} sx={{flexGrow: 1}}>
                            <CostAwareDailySavingsTab dailySavings={dailySavings}/>
                        </Box>
                        <Box id={"monthly-savings-tabpanel"} aria-labelledby={"monthly-savings-tab"} hidden={selectedTab !== 1} sx={{flexGrow: 1}}>
                            <CostAwareMonthlySavingsTab monthlySavings={monthlySavings}/>
                        </Box>
                        <Box id={"yearly-savings-tabpanel"} aria-labelledby={"yearly-savings-tab"} hidden={selectedTab !== 2} sx={{flexGrow: 1}}>
                            <CostAwareYearlySavingsTab yearlySavings={yearlySavings}/>
                        </Box>
                    </Box>
                </MuiDialogContent>
            </Dialog>
        </>
    )
}

export default CostAwareSavings;