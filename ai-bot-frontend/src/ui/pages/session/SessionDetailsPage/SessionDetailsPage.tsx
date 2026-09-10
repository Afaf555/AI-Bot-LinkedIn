import { Box, Chip, CircularProgress, Stack, Typography } from '@mui/material';
import { useParams } from 'react-router';
import useSessionDetails from '../../../../hooks/useSessionDetails.ts';
import SessionLogViewer from '../../../components/session/SessionLogViewer/SessionLogViewer.tsx';

const formatTimestamp = (value: string | null) => value ? new Date(value).toLocaleString() : '—';

const SessionDetailsPage = () => {
    const { id } = useParams<{ id: string }>();
    const { session, logs, loading } = useSessionDetails(id!);

    if (loading) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress/></Box>;
    }
    if (!session) {
        return <Typography color='text.secondary'>Session #{id} not found.</Typography>;
    }

    return (
        <Box>
            <Typography variant='h5' gutterBottom>Session #{id} — {session.socialNetwork}</Typography>
            <Stack spacing={0.5} sx={{ mb: 3 }}>
                <Box><Chip label={session.status} size='small'/></Box>
                <Typography color='text.secondary'>{session.description}</Typography>
                <Typography variant='body2' color='text.secondary'>
                    Targets: {session.targets.map((t) => `${t.type}:${t.value}`).join(', ') || '—'}
                </Typography>
                <Typography variant='body2' color='text.secondary'>
                    Started: {formatTimestamp(session.startedAt)} · Finished: {formatTimestamp(session.finishedAt)}
                </Typography>
            </Stack>
            <Typography variant='h6' gutterBottom>Bot Action Trace</Typography>
            <SessionLogViewer logs={logs}/>
        </Box>
    );
};

export default SessionDetailsPage;