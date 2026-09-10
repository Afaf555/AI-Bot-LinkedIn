import {
    Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, MenuItem, Stack, TextField
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import { useState } from 'react';
import type { CreateTargetRequest, SocialNetwork, TargetType } from '../../../../api/types/session.ts';
import useSessions from '../../../../hooks/useSessions.ts';

interface StartSessionDialogProps {
    open: boolean;
    onClose: () => void;
}

const socialNetworks: SocialNetwork[] = [
    'FACEBOOK', 'INSTAGRAM', 'X', 'REDDIT', 'TIKTOK', 'YOUTUBE', 'THREADS', 'LINKEDIN'
];
const targetTypes: TargetType[] = ['PROFILE', 'HASHTAG', 'KEYWORD', 'FEED_URL'];
const emptyTarget: CreateTargetRequest = { type: 'PROFILE', value: '' };

const StartSessionDialog = ({ open, onClose }: StartSessionDialogProps) => {
    const { onCreate } = useSessions();

    const [socialNetwork, setSocialNetwork] = useState<SocialNetwork>('LINKEDIN');
    const [description, setDescription] = useState<string>('');
    const [targets, setTargets] = useState<CreateTargetRequest[]>([{ ...emptyTarget }]);
    const [submitting, setSubmitting] = useState<boolean>(false);

    const handleTargetChange = (index: number, patch: Partial<CreateTargetRequest>) => {
        setTargets((prev) => prev.map((t, i) => (i === index ? { ...t, ...patch } : t)));
    };

    const handleAddTarget = () => setTargets((prev) => [...prev, { ...emptyTarget }]);
    const handleRemoveTarget = (index: number) => setTargets((prev) => prev.filter((_, i) => i !== index));

    const reset = () => {
        setSocialNetwork('LINKEDIN');
        setDescription('');
        setTargets([{ ...emptyTarget }]);
    };

    const handleClose = () => { reset(); onClose(); };

    const isValid = description.trim() !== '' && targets.every((t) => t.value.trim() !== '');

    const handleSubmit = async () => {
        if (!isValid) return;
        setSubmitting(true);
        try {
            await onCreate({ socialNetwork, description, targets });
            handleClose();
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Dialog open={open} onClose={handleClose} fullWidth maxWidth='sm'>
            <DialogTitle>New Extraction Session</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ mt: 1 }}>
                    <TextField select label='Social Network' value={socialNetwork}
                               onChange={(e) => setSocialNetwork(e.target.value as SocialNetwork)}>
                        {socialNetworks.map((network) => <MenuItem key={network} value={network}>{network}</MenuItem>)}
                    </TextField>

                    <TextField label='Description' value={description}
                               onChange={(e) => setDescription(e.target.value)} multiline minRows={2} required/>

                    <Stack spacing={1}>
                        {targets.map((target, index) => (
                            <Box key={index} sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                                <TextField select label='Type' value={target.type}
                                           onChange={(e) => handleTargetChange(index, { type: e.target.value as TargetType })}
                                           sx={{ minWidth: 140 }}>
                                    {targetTypes.map((type) => <MenuItem key={type} value={type}>{type}</MenuItem>)}
                                </TextField>
                                <TextField label='Value' value={target.value}
                                           onChange={(e) => handleTargetChange(index, { value: e.target.value })} fullWidth required/>
                                <IconButton onClick={() => handleRemoveTarget(index)} disabled={targets.length === 1} aria-label='remove target'>
                                    <DeleteIcon/>
                                </IconButton>
                            </Box>
                        ))}
                        <Button startIcon={<AddIcon/>} onClick={handleAddTarget} sx={{ alignSelf: 'flex-start' }}>
                            Add Target
                        </Button>
                    </Stack>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose}>Cancel</Button>
                <Button variant='contained' disabled={!isValid || submitting} onClick={handleSubmit}>
                    Create
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default StartSessionDialog;