import React from 'react';
import {Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
    dialogActions: {
        padding: '1rem'
    }
}));

export default function GithubStarPopup() {

    const classes = useStyles();
    const [visible, setVisible] = React.useState(false);


    React.useEffect(() => {
        let popupShown = localStorage.getItem('githubStarPopupShown');
        if (popupShown === 'true') {
            return;
        }

        let firstUsage = localStorage.getItem('githubStarPopupFirstUsage');
        if (!firstUsage) {
            localStorage.setItem('githubStarPopupFirstUsage', new Date().toISOString());
        } else {
            let firstUsageDate = Date.parse(firstUsage);
            let nowDate = new Date();
            const diffTime = Math.abs(nowDate - firstUsageDate);
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            if (diffDays >= 8) {
                setVisible(true);
            }
        }
    }, []);

    const remindMeLater = () => {
        localStorage.setItem('githubStarPopupFirstUsage', new Date().toISOString());
        setVisible(false);
    }

    const handleClose = () => {
        localStorage.setItem('githubStarPopupShown', 'true');
        setVisible(false);
    }

    if (!visible) return null;


    return (
        <Dialog
            open={visible}
            onClose={handleClose}
            aria-labelledby="github-dialog-title"
            aria-describedby="github-dialog-description"
        >
            <DialogTitle id="github-dialog-title">
                👋 Can I ask you something?
            </DialogTitle>
            <DialogContent dividers>
                <DialogContentText id="github-dialog-description">
                    You've been using JobRunr for about a week now - I hope you like it! <br/><br/>
                    If JobRunr makes your life easier, could you please <a
                    href="https://github.com/jobrunr/jobrunr/stargazers" target="_blank" rel="noreferrer">star the
                    project
                    on Github</a> (if you have not done so already) or write a small success story in the <a
                    href="https://github.com/jobrunr/jobrunr/discussions/categories/show-and-tell">Show and
                    tell</a> category of <a href="https://github.com/jobrunr/jobrunr/discussions" target="_blank"
                                            rel="noreferrer">Github discussions</a>? <em>I would really appreciate
                    it!</em> 🙏<br/><br/>
                    If not, can you please start a <a href="https://github.com/jobrunr/jobrunr/discussions"
                                                      target="_blank" rel="noreferrer">Github discussion</a> and tell me
                    what's giving you a hard time?<br/><br/>
                    Thanks for your support!
                </DialogContentText>
            </DialogContent>
            <DialogActions className={classes.dialogActions}>
                <Button onClick={remindMeLater} color="inherit">
                    remind me later
                </Button>
                <Button onClick={handleClose} color="inherit" variant="contained">
                    Dismiss
                </Button>
            </DialogActions>
        </Dialog>
    )
}