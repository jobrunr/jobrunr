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
    Snackbar,
    TextField
} from "@mui/material";
import {styled} from "@mui/material/styles";
import {Event, ExpandMoreOutlined} from "@mui/icons-material";
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import {Magnify} from "mdi-material-ui";
import {useEffect, useRef, useState} from "react";
import Dialog from "@mui/material/Dialog";
import Button from "@mui/material/Button";
import Box from "@mui/material/Box";
import Alert from "@mui/material/Alert";
import {useLocation} from "react-router";

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

const TryProDialog = ({open, setOpen, setFormSubmitted}) => {
    const location = useLocation();

    const urlSearchParams = new URLSearchParams(location.search);
    const jobState = urlSearchParams.get('state') ?? 'ENQUEUED';

    const formRef = useRef(null);
    const [errorText, setErrorText] = useState(undefined);
    const [numberOfJobs, setNumberOfJobs] = useState(undefined);
    const handleClose = () => {
        setErrorText(undefined);
        setOpen(false);
    }

    useEffect(() => {
        if (open) {
            let url = `/api/jobs?state=${jobState.toUpperCase()}&limit=1000`;
            fetch(url,)
                .then(res => res.json())
                .then(response => {
                    setNumberOfJobs(response.total);
                })
                .catch(error => console.log(error));
        }
    }, [open]);

    const submitForm = () => {
        const formData = new FormData(formRef.current);
        const email = formData.get("email");
        const company = formData.get("company");

        if (!email || !company) {
            setErrorText("Please include both your email and company");
            return;
        } else {
            setErrorText(undefined);
        }

        fetch("https://n8n.srv851199.hstgr.cloud/webhook/f7a5e38e-4b1d-4f5b-b534-e014ff6b80fe", {
            method: "POST",
            body: JSON.stringify({
                "email": email,
                "company": company,
                "username": "",
                "form": "trial",
                "utm_source": "oss-dashboard",
                "utm_medium": "pop-up"
            }),
            headers: {
                "Content-type": "application/json"
            }
        }).then((response) => {
            if (response.status === 200) {
                setFormSubmitted(true);
                handleClose();
            } else {
                setErrorText("Something went wrong submitting the form, please try again");
            }
        });
    }

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            aria-labelledby="try-pro-dialog-title"
            aria-describedby="try-pro-dialog-description"
        >
            <DialogTitle id="try-pro-dialog-title">
                Search all {numberOfJobs ? ((numberOfJobs > 999 ? "999+" : numberOfJobs) + " " + jobState.toLowerCase()) : "your"} jobs in this dashboard
            </DialogTitle>
            <DialogContent dividers>
                <DialogContentText id="try-pro-dialog-description">
                    Job search and filters are part of <a
                    href="https://www.jobrunr.io/en/documentation/pro/jobrunr-pro-dashboard/" target="_blank"
                    rel="noreferrer" title="Support the development of JobRunr by getting a Pro license!">JobRunr
                    Pro</a>!<br/><br/>

                    Leave your email and we will send you a Pro trial key, so you can use these filters, priority queues, workflows, and so much more.
                </DialogContentText>
                <form ref={formRef} onSubmit={submitForm} style={{marginTop: "1rem"}}>
                    <Box sx={{
                        display: "grid",
                        gridTemplateColumns: "auto 1fr",
                        gap: 1,
                        alignItems: "center",
                        textAlign: "right"
                    }}>
                        <Typography>Email</Typography>
                        <TextField name="email" id="form-email" size="small" fullWidth/>

                        <Typography>Company</Typography>
                        <TextField name="company" id="form-company" size="small" fullWidth/>
                    </Box>

                    {errorText && <Typography color="error" sx={{mt: 1}}>{errorText}</Typography>}
                </form>
            </DialogContent>
            <DialogActions sx={{px: '1rem', pt: '1rem', pb: 0}} style={{justifyContent: "start"}}>
                <Button onClick={submitForm} variant="contained" color="inherit" sx={{backgroundColor: "#00F0B5"}}>
                    Unlock job search
                </Button>
                <Button onClick={handleClose} color="inherit" variant="contained">
                    Dismiss
                </Button>
            </DialogActions>
            <DialogContent>
                <Typography sx={{fontStyle: "italic"}} variant="caption">
                    We use your address to send the key and to help you get it running. Nothing else.</Typography>
                <br/>
                <Typography variant="caption">Rather talk to a person first? Mail <a
                    href="mailto:hello@jobrunr.io?subject=JobRunr Pro Trial Request">
                    hello@jobrunr.io</a>
                </Typography>
            </DialogContent>
        </Dialog>
    );
}

export const JobsFilterPanel = ({}) => {
    const [showDialog, setShowDialog] = useState(false);
    const [formSubmitted, setFormSubmitted] = useState(false);

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
            <TryProDialog open={showDialog} setOpen={setShowDialog} setFormSubmitted={setFormSubmitted}/>
            <Snackbar open={formSubmitted}
                      autoHideDuration={5000}
                      onClose={() => setFormSubmitted(false)}
                      anchorOrigin={{vertical: "bottom", horizontal: "center"}}
            >
                <Alert severity="success">
                    Our team is preparing your license and you can expect it in your inbox very soon.
                </Alert>
            </Snackbar>
        </>
    );
};