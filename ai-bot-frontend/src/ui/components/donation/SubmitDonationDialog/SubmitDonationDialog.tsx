import {
    Box, Button, Checkbox, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
    List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography
} from '@mui/material';
import { useEffect, useState } from 'react';
import type { PostResponse } from '../../../../api/types/post.ts';
import postApi from '../../../../api/postApi.ts';
import useDonations from '../../../../hooks/useDonations.ts';
import useSnackbar from '../../../../hooks/useSnackbar.ts';

interface SubmitDonationDialogProps {
    open: boolean;
    onClose: () => void;
}

const SubmitDonationDialog = ({ open, onClose }: SubmitDonationDialogProps) => {
    const { showSnackbar } = useSnackbar();
    const { onCreate } = useDonations();

    const [posts, setPosts] = useState<PostResponse[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [selected, setSelected] = useState<number[]>([]);
    const [submitting, setSubmitting] = useState<boolean>(false);

    useEffect(() => {
        if (!open) return;
        setLoading(true);
        void (async () => {
            try {
                const response = await postApi.findAll({ donated: false }, 0, 100);
                setPosts(response.data.content);
            } catch (err) {
                showSnackbar(err instanceof Error ? err.message : 'Failed to load posts.', 'error');
            } finally {
                setLoading(false);
            }
        })();
    }, [open, showSnackbar]);

    const toggle = (id: number) => {
        setSelected((prev) => (prev.includes(id) ? prev.filter((p) => p !== id) : [...prev, id]));
    };

    const handleClose = () => { setSelected([]); onClose(); };

    const handleSubmit = async () => {
        if (selected.length === 0) return;
        setSubmitting(true);
        try {
            await onCreate({ postIds: selected });
            handleClose();
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Dialog open={open} onClose={handleClose} fullWidth maxWidth='md'>
            <DialogTitle>New Donation Batch</DialogTitle>
            <DialogContent>
                {loading && <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}><CircularProgress/></Box>}
                {!loading && posts.length === 0 && (
                    <Typography color='text.secondary'>No un-donated posts available. Extract more content first.</Typography>
                )}
                {!loading && posts.length > 0 && (
                    <List dense>
                        {posts.map((post) => (
                            <ListItem key={post.id} disablePadding>
                                <ListItemButton onClick={() => toggle(post.id)}>
                                    <ListItemIcon>
                                        <Checkbox edge='start' checked={selected.includes(post.id)} tabIndex={-1}/>
                                    </ListItemIcon>
                                    <ListItemText primary={post.authorHandle ?? 'unknown author'}
                                                  secondary={post.content ?? 'No content extracted.'}
                                                  slotProps={{ secondary: { noWrap: true } }}/>
                                </ListItemButton>
                            </ListItem>
                        ))}
                    </List>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose}>Cancel</Button>
                <Button variant='contained' disabled={selected.length === 0 || submitting} onClick={handleSubmit}>
                    Create Batch ({selected.length})
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default SubmitDonationDialog;