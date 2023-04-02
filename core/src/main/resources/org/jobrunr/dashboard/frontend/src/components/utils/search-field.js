import React from 'react';
import {IconButton, InputAdornment, TextField} from "@material-ui/core";
import {Magnify} from "mdi-material-ui";

const SearchField = (props) => {

    const search = (ev) => {
        if(ev.key === 'Enter') {
            props.onSearch(ev.target.value);
        } else if(ev.type === 'click') {
            let element = ev.target.parentElement;
            console.log(element);
            while(element.previousSibling === null || element.previousSibling.nodeName !== 'INPUT') {
                element = element.parentElement;
            }
            console.log(element);
            props.onSearch(element.previousSibling.value);
        }
    }

    return (
        <TextField id={props.id} label={props.label} onKeyPress={search}
                   error={props.hasError}
                   helperText={props.hasError ? props.errorText : null}
                   defaultValue={props.defaultValue}
                   InputProps={{
                       endAdornment: <InputAdornment position="end"><IconButton
                           aria-label="search"
                           onClick={search}
                           edge="end"
                       ><Magnify /></IconButton></InputAdornment>
                   }} fullWidth/>
    );
};

export default SearchField;