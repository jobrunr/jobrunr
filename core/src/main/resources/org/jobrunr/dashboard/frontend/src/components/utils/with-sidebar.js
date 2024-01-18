import { useState } from 'react';
import { styled } from "@mui/material/styles";
import Drawer from "@mui/material/Drawer";
import {IconButton} from "@mui/material";
import {ChevronLeft, ChevronRight} from "mdi-material-ui";

const StyledDrawer = styled(Drawer)(({theme, collapsed}) => ({
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

const WithSidebar = (Sidebar, Component) => {
    const [collapsed, setCollapsed] = useState(false);
    return (props) => (
        <div style={{display: "flex"}}>
            <StyledDrawer variant="permanent" collapsed={collapsed}>
                <Toolbar />
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
            <Component />
        </div>
    );
}

export default WithSidebar;