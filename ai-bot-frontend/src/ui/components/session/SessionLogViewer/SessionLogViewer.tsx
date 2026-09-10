import {
    Box, Chip, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import type { BotActionLogResponse } from '../../../../api/types/session.ts';

interface SessionLogViewerProps {
    logs: BotActionLogResponse[];
}

const SessionLogViewer = ({ logs }: SessionLogViewerProps) => {
    if (logs.length === 0) {
        return <Box><Typography color='text.secondary'>No bot actions recorded yet.</Typography></Box>;
    }

    return (
        <TableContainer component={Paper}>
            <Table size='small'>
                <TableHead>
                    <TableRow>
                        <TableCell>Action</TableCell>
                        <TableCell>Details</TableCell>
                        <TableCell align='center'>Result</TableCell>
                        <TableCell>Occurred At</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {logs.map((log) => (
                        <TableRow key={log.id}>
                            <TableCell><Chip label={log.actionType} size='small'/></TableCell>
                            <TableCell>{log.details ?? '—'}</TableCell>
                            <TableCell align='center'>
                                {log.successful ? <CheckCircleIcon color='success' fontSize='small'/> : <CancelIcon color='error' fontSize='small'/>}
                            </TableCell>
                            <TableCell>{new Date(log.occurredAt).toLocaleString()}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
};

export default SessionLogViewer;