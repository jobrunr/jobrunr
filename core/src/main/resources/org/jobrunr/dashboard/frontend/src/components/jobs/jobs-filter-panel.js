import Typography from '@mui/material/Typography';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Checkbox,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Grid,
    IconButton,
    InputAdornment,
    TextField
} from "@mui/material";
import {styled} from "@mui/material/styles";
import {Event, ExpandMoreOutlined} from "@mui/icons-material";
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import {Magnify} from "mdi-material-ui";
import {useState} from "react";
import Dialog from "@mui/material/Dialog";
import Button from "@mui/material/Button";

const Heading = styled(Typography)(({theme}) => ({
    fontSize: theme.typography.pxToRem(15),
    flexBasis: '8%',
    flexShrink: 0,
    margin: theme.spacing(1)
}))

const SecondaryHeading = styled("span")(({theme}) => ({
    fontSize: theme.typography.pxToRem(15),
    color: theme.palette.text.secondary,
    margin: theme.spacing(1)
}))

const FilterField = ({decorationIcon: Icon = Magnify, setShowDialog, ...rest}) => {
    return (
        <TextField
            variant="standard"
            fullWidth
            onClick={() => setShowDialog(true)}
            slotProps={{
                input: {
                    endAdornment: <InputAdornment position="end">
                        <IconButton aria-label="search" disabled edge="end" size="large"><Icon/></IconButton>
                    </InputAdornment>,
                    readOnly: true,
                }
            }}
            {...rest}
        />
    );
};

const TryProDialog = ({open, setOpen}) => {
    const handleClose = () => {
        setOpen(false);
    }

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            aria-labelledby="try-pro-dialog-title"
            aria-describedby="try-pro-dialog-description"
        >
            <DialogTitle id="try-pro-dialog-title">
                Try JobRunr Pro
            </DialogTitle>
            <DialogContent dividers>
                {/* TODO Improve copy with Nicholas */}
                <DialogContentText id="try-pro-dialog-description">
                    Are you trying to find a certain job faster? Use filters which are available in <a
                    href="https://www.jobrunr.io/en/documentation/pro/jobrunr-pro-dashboard/" target="_blank"
                    rel="noreferrer" title="Support the development of JobRunr by getting a Pro license!">JobRunr
                    Pro</a>!<br/><br/>

                    Filters and so much more available in JobRunr Pro, try it below!
                </DialogContentText>
            </DialogContent>
            <DialogActions style={{padding: '1rem'}}>
                <Button onClick={handleClose} variant="contained" color="inherit" target={"_blank"} href={"https://www.jobrunr.io/en/start-jobrunr-pro/?step=1"}
                        sx={{backgroundColor: "#00F0B5"}}>
                    Try Pro
                </Button>
                <Button onClick={handleClose} color="inherit" variant="contained">
                    Dismiss
                </Button>
            </DialogActions>
        </Dialog>
    );
}

export const JobsFilterPanel = ({}) => {
    const [showDialog, setShowDialog] = useState(false);

    return (
        <>
            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandMoreOutlined/>}
                    aria-controls="job-filter-panel-content"
                    id="job-filter-panel-header"
                >
                    <Heading as="span">Filters</Heading>
                    <SecondaryHeading>No filters selected</SecondaryHeading>
                </AccordionSummary>
                <AccordionDetails>
                    <Grid container>
                        <Grid container spacing={3} size={{marginBottom: '1em', xs: 12}}>
                            <Grid size={4}>
                                <FilterField id="job-name" label="Job name" setShowDialog={setShowDialog} placeholder={"doWork()"}/>
                            </Grid>
                            <Grid size={4}>
                                <FilterField id="job-id" label="Job id" setShowDialog={setShowDialog} placeholder={"436e3650-dc3e-43c4-b585-7a9995f19208"}/>
                            </Grid>
                            <Grid size={4}>
                                <FilterField id="rate-limiter" label="Rate limiter" setShowDialog={setShowDialog} placeholder={"rate-limiter-name"}/>
                            </Grid>
                        </Grid>
                        <Grid container spacing={3} size={{marginBottom: '1em', xs: 12}}>
                            <Grid size={4}>
                                <FilterField label="Job signature" id="job-signature-select-label" setShowDialog={setShowDialog}
                                             decorationIcon={ArrowDropDownIcon}/>
                            </Grid>
                            <Grid size={4}>
                                <FilterField label="Job exception type" id="job-exception-select-label" setShowDialog={setShowDialog}
                                             decorationIcon={ArrowDropDownIcon}/>
                            </Grid>
                            <Grid size={4}>
                                <FilterField id="job-fingerprint" label="Job fingerprint" setShowDialog={setShowDialog}/>
                            </Grid>
                        </Grid>
                        <Grid container spacing={3} size={{marginBottom: '1em', xs: 12}}>
                            <Grid size={3}>
                                <FilterField id="recurring-job-id" label="Recurring job id" setShowDialog={setShowDialog}
                                             placeholder={"436e3650-dc3e-43c4-b585-7a9995f19208"}/>
                            </Grid>
                            <Grid size={3}>
                                <FilterField id="awaiting-on-job-id" label="Awaiting on job id" setShowDialog={setShowDialog}
                                             placeholder={"436e3650-dc3e-43c4-b585-7a9995f19208"}/>
                            </Grid>
                            <Grid size={2}>
                                <FilterField id="job-label" label="Job label" setShowDialog={setShowDialog} placeholder={"some-label"}/>
                            </Grid>
                            <Grid size={2}>
                                <FilterField label="Queue" id="queue-select-label" setShowDialog={setShowDialog} decorationIcon={ArrowDropDownIcon}/>
                            </Grid>
                            <Grid size={2}>
                                <FilterField label="Server tag" id="server-tags-select-label" setShowDialog={setShowDialog} decorationIcon={ArrowDropDownIcon}/>
                            </Grid>
                        </Grid>
                        <Grid container spacing={12} size={12}>
                            <Grid size={2.4}>
                                <FilterField label="Created after" id="created-after-picker" setShowDialog={setShowDialog} decorationIcon={Event}/>
                            </Grid>
                            <Grid size={2.4}>
                                <FilterField label="Created before" id="created-before-picker" setShowDialog={setShowDialog} decorationIcon={Event}/>
                            </Grid>
                            <Grid size={2.4}>
                                <FilterField label="Updated after" id="updated-after-picker" setShowDialog={setShowDialog} decorationIcon={Event}/>
                            </Grid>
                            <Grid size={2.4}>
                                <FilterField label="Updated before" id="updated-before-picker" setShowDialog={setShowDialog} decorationIcon={Event}/>
                            </Grid>
                            <Grid size={2.4} sx={{display: "flex", alignItems: "center", pt: 1}} onClick={() => setShowDialog(true)}>
                                <Checkbox indeterminate={true}/>
                                <Typography>Batch jobs only</Typography>
                            </Grid>
                        </Grid>
                    </Grid>
                </AccordionDetails>
            </Accordion>
            <TryProDialog open={showDialog} setOpen={setShowDialog}/>
        </>
    );
};