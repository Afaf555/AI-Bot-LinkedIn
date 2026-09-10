import { Button, Card, CardActions, CardContent, Chip, Stack, Typography } from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import InfoIcon from '@mui/icons-material/Info';
import { useNavigate } from 'react-router';
import type { SessionResponse, SessionStatus } from '../../../../api/types/session.ts';
import useSessions from '../../../../hooks/useSessions.ts';

interface SessionCardProps {
  session: SessionResponse;
}

const statusColor: Record<SessionStatus, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  CREATED: 'default',
  RUNNING: 'info',
  PAUSED: 'warning',
  COMPLETED: 'success',
  FAILED: 'error'
};

const formatTimestamp = (value: string | null) =>
    value ? new Date(value).toLocaleString() : '—';

const SessionCard = ({ session }: SessionCardProps) => {
  const navigate = useNavigate();
  const { onStart, onStop } = useSessions();

  const canStart = session.status === 'CREATED' || session.status === 'PAUSED';
  const canStop = session.status === 'RUNNING';

  return (
      <Card sx={{ maxWidth: 300, height: '100%', display: 'flex', flexDirection: 'column' }}>
        <CardContent sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
          <Typography variant='h5'>{session.socialNetwork}</Typography>
          <Typography variant='subtitle1' sx={{ flexGrow: 1 }}>{session.description}</Typography>
          <Chip
              label={session.status}
              color={statusColor[session.status]}
              size='small'
              sx={{ alignSelf: 'flex-start', mb: 1 }}
          />
          <Stack spacing={0.5}>
            <Typography variant='caption' color='text.secondary'>
              {session.targets.length} target(s): {session.targets.map((t) => t.value).join(', ') || '—'}
            </Typography>
            <Typography variant='caption' color='text.secondary'>
              Started: {formatTimestamp(session.startedAt)}
            </Typography>
            <Typography variant='caption' color='text.secondary'>
              Finished: {formatTimestamp(session.finishedAt)}
            </Typography>
          </Stack>
        </CardContent>
        <CardActions sx={{ justifyContent: 'space-between' }}>
          <Button startIcon={<InfoIcon/>} onClick={() => navigate(`/sessions/${session.id}`)}>
            Info
          </Button>
          <Button startIcon={<PlayArrowIcon/>} color='success' disabled={!canStart} onClick={() => onStart(session.id)}>
            Start
          </Button>
          <Button startIcon={<StopIcon/>} color='error' disabled={!canStop} onClick={() => onStop(session.id)}>
            Stop
          </Button>
        </CardActions>
      </Card>
  );
};

export default SessionCard;