import {MarkEmailRead, Notifications} from "@mui/icons-material";
import IconButton from "@mui/material/IconButton";
import {alpha, Badge, Divider, ListItem, Stack} from "@mui/material";
import {useCallback, useRef, useState} from "react";
import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Button from "@mui/material/Button";
import {ClickAwayPopper} from "../components/ui/ClickAwayPopper";
import List from "@mui/material/List";
import {styled} from "@mui/material/styles";
import ListItemText from "@mui/material/ListItemText";
import Typography from "@mui/material/Typography";
import Chip from "@mui/material/Chip";


const NotificationListItem = styled(ListItem)(({theme, unread}) => ({
    backgroundColor: unread
        ? alpha(theme.palette.primary.main, 0.04)
        : "transparent",
    borderLeft: unread
        ? `4px solid ${theme.palette.primary.main}`
        : "4px solid transparent",
    "&:hover": {
        backgroundColor: alpha(theme.palette.action.hover, 0.08),
    },
    cursor: "pointer",
    paddingLeft: theme.spacing(2),
    paddingRight: theme.spacing(1),
}));

export const NotificationsCenter = () => {
    const [isOpen, setIsOpen] = useState(false);
    const popperAnchorEl = useRef(null);

    const openClusterSelector = useCallback(() => {
        setIsOpen(true);
    }, []);

    const closeClusterSelector = useCallback(() => {
        setIsOpen(false);
    }, []);

    return <>
        <IconButton
            edge="start"
            color="inherit"
            size="large"
            sx={{marginRight: 2}}
            onClick={openClusterSelector}
            ref={popperAnchorEl}
        >
            <Badge badgeContent={2} max={99} color="secondary" style={{textTransform: "uppercase"}}>
                <Notifications/>
            </Badge>
        </IconButton>
        <ClickAwayPopper isOpen={isOpen} handleClickAway={closeClusterSelector} anchorEl={popperAnchorEl?.current}>
            <Paper elevation={6}>
                <Box width="80vw" maxWidth={500} maxHeight="70vh" overflow="auto">
                    <Box p={2}>
                        <Stack direction="row" spacing={1} justifyContent={"space-between"}>
                            <Button startIcon={<MarkEmailRead/>} variant="outlined">Mark all read</Button>
                            <Button variant="outlined" onClick={closeClusterSelector}>Close</Button>
                        </Stack>
                    </Box>
                    <List sx={{p: 0}}>
                        <Divider/>
                        <NotificationListItem>
                            <ListItemText
                                primary={
                                    <Typography variant="subtitle2" fontWeight={700}>Fatal</Typography>
                                }
                                secondary={
                                    <Box>
                                        <Typography
                                            variant="body2"
                                            color="text.secondary"
                                            sx={{mb: 1, lineHeight: 1.4}}
                                        >
                                            JobRunr encountered an exception that should not happen that could result in the Background Job Servers stopping.
                                            You need to look into this.
                                        </Typography>
                                        <Stack
                                            direction="row"
                                            alignItems="center"
                                            spacing={1}
                                            flexWrap="wrap"
                                        >
                                            <Chip
                                                label={"New"}
                                                size="small"
                                                variant="outlined"
                                                sx={{height: 20, fontSize: "0.7rem"}}
                                            />
                                        </Stack>
                                    </Box>
                                }
                            />
                        </NotificationListItem>
                        <Divider/>
                        <NotificationListItem>
                            <ListItemText
                                primary={
                                    <Stack>
                                        <Typography variant="subtitle2" fontWeight={700}>Fatal</Typography>
                                    </Stack>
                                }
                                secondary={
                                    <Box>
                                        <Typography
                                            variant="body2"
                                            color="text.secondary"
                                            sx={{mb: 1, lineHeight: 1.4}}
                                        >
                                            JobRunr encountered an exception that should not happen that could result in the Background Job Servers stopping.
                                            You need to look into this.
                                        </Typography>
                                        <Stack
                                            direction="row"
                                            alignItems="center"
                                            spacing={1}
                                            flexWrap="wrap"
                                        >
                                            <Chip
                                                label={"New"}
                                                size="small"
                                                variant="outlined"
                                                sx={{height: 20, fontSize: "0.7rem"}}
                                            />
                                        </Stack>
                                    </Box>
                                }
                            />
                        </NotificationListItem>
                    </List>
                </Box>
            </Paper>
        </ClickAwayPopper>
    </>
}