import {ClickAwayListener, Popper, Unstable_TrapFocus as TrapFocus} from "@mui/material";

export const ClickAwayPopper = ({children, isOpen, handleClickAway, ...rest}) => {
    return (
        isOpen &&
        <ClickAwayListener onClickAway={handleClickAway}>
            <Popper
                aria-labelledby="jobrunr-clusters-menu"
                sx={{zIndex: 1250}} // TODO use theme to determine z-index
                open
                placement="bottom-end"
                {...rest}
            >
                <TrapFocus open disableAutoFocus disableEnforceFocus>
                    {children}
                </TrapFocus>
            </Popper>
        </ClickAwayListener>
    )
}