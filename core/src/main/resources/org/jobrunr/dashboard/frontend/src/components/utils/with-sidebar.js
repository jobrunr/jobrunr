import {useState} from 'react';
import {styled} from "@mui/material/styles";
import Drawer from "@mui/material/Drawer";
import {IconButton} from "@mui/material";
import {ChevronLeft, ChevronRight} from "mdi-material-ui";
import useMediaQuery from "@mui/material/useMediaQuery";
import Sidebar from "../jobs/sidebar.js";

const drawerWidth = 260;

const openedMixin = (theme) => ({
    width: drawerWidth,
    transition: theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.enteringScreen,
    }),
});

const closedMixin = (theme) => ({
    width: `calc(${theme.spacing(8)} + 1px)`,
    transition: theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
});

const StyledDrawer = styled(Drawer, {shouldForwardProp: prop => prop !== "collapsed"})(
    ({theme, collapsed}) => ({
        flexShrink: 0,
        whiteSpace: 'nowrap',
        overflowX: 'hidden',
        ...(collapsed
            ? {...closedMixin(theme), "& .MuiDrawer-paper": closedMixin(theme)}
            : {...openedMixin(theme), "& .MuiDrawer-paper": openedMixin(theme)}),
    })
);

const Toggle = styled("div", {shouldForwardProp: prop => prop !== "collapsed"})(
    ({theme, collapsed}) => ({
        display: 'flex',
        alignItems: 'center',
        justifyContent: collapsed ? 'center' : 'flex-end',
        paddingRight: collapsed ? 0 : theme.spacing(0.5),
    })
);

const SidebarContainer = styled("div")({
    flexGrow: 1,
    overflowY: 'auto'
});

const Toolbar = styled("div")(({theme}) => ({
    ...theme.mixins.toolbar
}));

const SidebarDrawer = (props) => {
    const isSmallScreen = useMediaQuery((theme) => theme.breakpoints.down('md'));
    const [manualCollapsed, setManualCollapsed] = useState(null);
    const collapsed = manualCollapsed ?? isSmallScreen;

    return (
        <StyledDrawer variant="permanent" collapsed={collapsed}>
            <Toolbar/>
            <SidebarContainer>
                <Sidebar {...props} collapsed={collapsed}/>
            </SidebarContainer>
            <Toggle collapsed={collapsed}>
                <IconButton
                    onClick={() => setManualCollapsed(prev => !prev)}
                    title="Toggle sidebar"
                    size={collapsed ? "medium" : "large"}>
                    {collapsed ? <ChevronRight/> : <ChevronLeft/>}
                </IconButton>
            </Toggle>
        </StyledDrawer>
    );
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