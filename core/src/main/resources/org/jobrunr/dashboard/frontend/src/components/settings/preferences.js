import {Box, Divider, Grid, IconButton, Paper, ToggleButton, ToggleButtonGroup, Typography, useColorScheme} from "@mui/material";
import {CalendarMonth, DarkMode, LightMode, Person, SettingsBrightness} from "@mui/icons-material";
import {ClickAwayPopper} from "../ui/ClickAwayPopper.js";
import React, {useRef, useState} from "react";
import {convertToBrowserDefaultDateStyle} from "../../utils/helper-functions.js";
import {styled} from "@mui/material/styles";
import {dateStyles, useDateStyles} from "../../hooks/useDateStyles.js";

const PreferenceHeading = ({title, icon, ...rest}) => {
    return <Grid container spacing={1} {...rest}>
        {icon} {title}
    </Grid>
}

const IconToggleButton = styled(ToggleButton)({
    display: 'flex',
    justifyContent: 'center',
    textTransform: 'none',
});

const DateStyleToggleButton = styled(ToggleButton)({
    alignItems: 'flex-start',
    flexDirection: 'column',
    textAlign: 'left',
    textTransform: 'none',
})

export const Preferences = () => {
    const [isOpen, setIsOpen] = useState(false);
    const popperAnchorEl = useRef(null);
    const [dateStyle, setDateStyle] = useDateStyles();
    const {mode, setMode} = useColorScheme();

    const currentDate = new Date();

    const openNotifications = () => {
        setIsOpen(true);
    };

    const closeNotifications = () => {
        setIsOpen(false);
    };

    const handleDateStyleChange = (_, newStyle) => {
        setDateStyle(newStyle);
    }

    return (
        <>
            <IconButton
                edge="start"
                color="inherit"
                size="large"
                sx={{marginRight: 1}}
                onClick={openNotifications}
                ref={popperAnchorEl}
                id="notifications-center-button"
            >
                <Person/>
            </IconButton>

            <ClickAwayPopper isOpen={isOpen} handleClickAway={closeNotifications} anchorEl={popperAnchorEl?.current}>
                <Paper elevation={6}>
                    <Box width="80vw" maxWidth={300} maxHeight="70vh" overflow="auto" p={2}>
                        <Grid container pb={2} spacing={2} direction="column">
                            <PreferenceHeading icon={<SettingsBrightness/>} title="Theme"/>

                            <ToggleButtonGroup
                                exclusive
                                value={mode}
                                onChange={(event) => {
                                    console.log(event.target);
                                    setMode(event.target.value)
                                }}
                                color="secondary"
                                fullWidth
                            >
                                <IconToggleButton value="light">
                                    <LightMode fontSize="small" sx={{marginRight: 1, pointerEvents: "none"}}/> Light
                                </IconToggleButton>
                                <IconToggleButton value="system">
                                    <SettingsBrightness fontSize="small" sx={{marginRight: 1, pointerEvents: "none"}}/> System
                                </IconToggleButton>
                                <IconToggleButton value="dark">
                                    <DarkMode fontSize="small" sx={{marginRight: 1, pointerEvents: "none"}}/> Dark
                                </IconToggleButton>
                            </ToggleButtonGroup>
                        </Grid>
                        <Divider/>
                        <Grid container pt={2} spacing={2} direction="column">
                            <PreferenceHeading icon={<CalendarMonth/>} title="Date style"/>
                            <ToggleButtonGroup
                                exclusive
                                value={dateStyle}
                                color="secondary"
                                orientation="vertical"
                                onChange={handleDateStyleChange}
                                fullWidth
                            >
                                <DateStyleToggleButton value={dateStyles.defaultStyle}>
                                    Time ago
                                    <Typography variant="caption" color="text.secondary">
                                        e.g., 1 minute ago
                                    </Typography>
                                </DateStyleToggleButton>
                                <DateStyleToggleButton value={dateStyles.localeStyle}>
                                    Locale date
                                    <Typography variant="caption" color="text.secondary">
                                        e.g., {currentDate.toLocaleString()}
                                    </Typography>
                                </DateStyleToggleButton>
                                <DateStyleToggleButton value={dateStyles.readableStyle}>
                                    Browser default
                                    <Typography variant="caption" color="text.secondary">
                                        e.g., {convertToBrowserDefaultDateStyle(currentDate)}
                                    </Typography>
                                </DateStyleToggleButton>
                                <DateStyleToggleButton value={dateStyles.iso8601Style}>
                                    ISO 8601
                                    <Typography variant="caption" color="text.secondary">
                                        e.g., {currentDate.toISOString()}
                                    </Typography>
                                </DateStyleToggleButton>
                            </ToggleButtonGroup>
                        </Grid>
                    </Box>
                </Paper>
            </ClickAwayPopper>
        </>
    )
}