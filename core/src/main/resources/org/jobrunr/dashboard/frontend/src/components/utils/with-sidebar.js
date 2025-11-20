import {useEffect, useState} from 'react';
import {styled} from "@mui/material/styles";
import Drawer from "@mui/material/Drawer";
import {IconButton} from "@mui/material";
import {ChevronLeft, ChevronRight} from "mdi-material-ui";
import useMediaQuery from "@mui/material/useMediaQuery";
import Sidebar from "../jobs/sidebar.js";

const StyledDrawer = styled(Drawer, {shouldForwardProp: prop => prop !== "collapsed"})
(({theme, collapsed}) => ({
    "&, & .MuiPaper-root": {
        width: collapsed ? `calc(${theme.spacing(7)} + 1px)` : 260,
        overflowX: collapsed ? 'hidden' : undefined,
    }
}));

const Toggle = styled("div")(({theme}) => ({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-end',
    paddingRight: theme.spacing(0.5),
    marginTop: 'auto'
}));

const Toolbar = styled("div")(({theme}) => ({
    ...theme.mixins.toolbar
}));

const SidebarDrawer = (props) => {
    const isSmallScreen = useMediaQuery((theme) => theme.breakpoints.down('md'));
    const [collapsed, setCollapsed] = useState(false);

    useEffect(() => {
        if (isSmallScreen) setCollapsed(true);
    }, [isSmallScreen]);

    return (
        <StyledDrawer variant="permanent" collapsed={collapsed}>
            <Toolbar/>
            <Sidebar {...props} />
            <Toggle>
                <IconButton
                    onClick={() => setCollapsed(!collapsed)}
                    title="Toggle sidebar"
                    size="large">
                    {collapsed ? <ChevronRight/> : <ChevronLeft/>}
                </IconButton>
            </Toggle>
        </StyledDrawer>
    )
}

const WithSidebar = (Sidebar, Component) => {
    return (props) => (
        <div style={{display: "flex"}}>
            <SidebarDrawer {...props} />
            <Component/>
        </div>
    );
}

export default WithSidebar;