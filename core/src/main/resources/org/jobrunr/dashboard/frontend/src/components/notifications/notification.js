import {alpha, ListItem, Menu, MenuItem, Stack} from "@mui/material";
import {styled} from "@mui/material/styles";
import {useState} from "react";
import Typography from "@mui/material/Typography";
import TimeAgo from "react-timeago/lib";
import IconButton from "@mui/material/IconButton";
import {MarkEmailRead, MarkEmailUnread, MoreVert} from "@mui/icons-material";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";
import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";

const getChipLabel = (severity) => {
    if (severity === "info") return "Info";
    if (severity === "warning") return "Warning";
    if (severity === "error") return "Critical";
    return "Unknown";
}

const NotificationListItem = styled(ListItem, {shouldForwardProp: (prop) => prop !== "read"})(({theme, read}) => ({
    backgroundColor: read
        ? "transparent"
        : alpha(theme.palette.primary.main, 0.04),
    borderLeft: read
        ? "4px solid transparent"
        : `4px solid ${theme.palette.primary.main}`,
    "&:hover": {
        backgroundColor: alpha(theme.palette.action.hover, 0.08),
    },
    paddingLeft: theme.spacing(2),
    paddingRight: theme.spacing(1),
}));

const NotificationTitle = ({title, date = undefined, extraMenuItems, read = false, onReadStatusToggled}) => {
    const [anchorEl, setAnchorEl] = useState(null);

    const openMenu = (e) => {
        stopPropagation(e);
        setAnchorEl(e.currentTarget);
    }

    const stopPropagation = (e) => {
        e.stopPropagation();
    }

    return (
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1} mb={1}>
            <div>
                <Typography variant="subtitle2" fontWeight={700} pt={0.5} sx={{textTransform: "none"}}>{title}</Typography>
                {date && <Typography variant="caption"><TimeAgo date={date} title={date.toString()}/></Typography>}
            </div>
            <IconButton ref={anchorEl} onClick={openMenu} sx={{padding: 0.5}}>
                <MoreVert fontSize="small"/>
            </IconButton>
            <Menu
                anchorEl={anchorEl}
                open={Boolean(anchorEl)}
                onClose={() => setAnchorEl(null)}
                anchorOrigin={{vertical: "bottom", horizontal: "right"}}
                transformOrigin={{vertical: "top", horizontal: "right"}}
                onClick={stopPropagation}
            >
                {read ? <MenuItem onClick={onReadStatusToggled}>
                        <ListItemIcon><MarkEmailUnread fontSize="small"/></ListItemIcon>
                        <ListItemText>Mark as unread</ListItemText>
                    </MenuItem>
                    : <MenuItem onClick={onReadStatusToggled}>
                        <ListItemIcon><MarkEmailRead fontSize="small"/></ListItemIcon>
                        <ListItemText>Mark as read</ListItemText>
                    </MenuItem>
                }
                {extraMenuItems}
            </Menu>
        </Stack>
    )
}

export const Notification = ({title = "Notification", date, severity = "warning", read, onReadStatusToggled, extraMenuItems, children}) => {
    return (
        <NotificationListItem read={read} onClick={onReadStatusToggled}>
            <ListItemText
                mt={0}
                primary={
                    <NotificationTitle
                        title={title}
                        extraMenuItems={extraMenuItems}
                        read={read}
                        onReadStatusToggled={onReadStatusToggled}
                        date={date}
                    />
                }
                secondary={
                    <Box>
                        <Box sx={{fontSize: "14px", color: "text.secondary", mb: 1}}>
                            {children}
                        </Box>
                        <Stack
                            direction="row"
                            alignItems="center"
                            spacing={1}
                            flexWrap="wrap"
                        >
                            <Chip
                                label={getChipLabel(severity)}
                                size="small"
                                color={severity}
                                sx={{fontSize: "0.7rem", textTransform: "capitalize"}}
                            />
                            {!read && <Chip
                                label="New"
                                size="small"
                                variant="outlined"
                                color="info"
                                sx={{fontSize: "0.7rem"}}
                            />}
                        </Stack>
                    </Box>
                }
                primaryTypographyProps={{component: "div"}}
                secondaryTypographyProps={{component: "div"}}
            />
        </NotificationListItem>
    )
}