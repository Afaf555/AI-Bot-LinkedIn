import { Box, FormControlLabel, MenuItem, Slider, Switch, TextField, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import type { PostFilter } from '../../../../api/types/post.ts';
import type { SessionResponse } from '../../../../api/types/session.ts';
import sessionApi from '../../../../api/sessionApi.ts';

interface PostFiltersProps {
    filter: PostFilter;
    onChange: (filter: PostFilter) => void;
}

const PostFilters = ({ filter, onChange }: PostFiltersProps) => {
    const [sessions, setSessions] = useState<SessionResponse[]>([]);
    const [search, setSearch] = useState<string>(filter.search ?? '');

    useEffect(() => {
        void (async () => {
            const response = await sessionApi.findAll();
            setSessions(response.data);
        })();
    }, []);

    const commitSearch = () => {
        onChange({ ...filter, search: search.trim() === '' ? undefined : search.trim() });
    };

    return (
        <Box sx={{ mb: 2, display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center' }}>
            <TextField select label='Session' size='small' sx={{ minWidth: 160 }}
                       value={filter.sessionId ?? ''}
                       onChange={(e) => onChange({ ...filter, sessionId: e.target.value === '' ? undefined : Number(e.target.value) })}>
                <MenuItem value=''>All sessions</MenuItem>
                {sessions.map((session) => (
                    <MenuItem key={session.id} value={session.id}>#{session.id} — {session.socialNetwork}</MenuItem>
                ))}
            </TextField>

            <Box sx={{ minWidth: 200 }}>
                <Typography variant='caption' color='text.secondary'>
                    Min. Macedonian confidence: {filter.minMacedonianConfidence ?? 0}
                </Typography>
                <Slider size='small' min={0} max={1} step={0.05}
                        value={filter.minMacedonianConfidence ?? 0}
                        onChange={(_, value) => onChange({ ...filter, minMacedonianConfidence: value as number })}/>
            </Box>

            <FormControlLabel
                control={
                    <Switch checked={filter.donated === true}
                            onChange={(e) => onChange({ ...filter, donated: e.target.checked ? true : undefined })}/>
                }
                label='Donated only'
            />

            <TextField label='Search content' size='small' value={search}
                       onChange={(e) => setSearch(e.target.value)}
                       onBlur={commitSearch}
                       onKeyDown={(e) => e.key === 'Enter' && commitSearch()}/>
        </Box>
    );
};

export default PostFilters;