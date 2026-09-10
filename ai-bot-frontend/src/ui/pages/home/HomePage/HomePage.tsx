import { Box, Card, CardContent, Chip, CircularProgress, Container, Grid, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import postApi from '../../../../api/postApi.ts';
import sessionApi from '../../../../api/sessionApi.ts';
import type { SessionResponse } from '../../../../api/types/session.ts';
import useDonations from '../../../../hooks/useDonations.ts';

const MACEDONIAN_CONFIDENCE_THRESHOLD = 0.8;

interface Stats {
  totalPosts: number;
  highConfidencePosts: number;
  latestSession: SessionResponse | null;
}

const HomePage = () => {
  const { donations, loading: donationsLoading } = useDonations();

  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    void (async () => {
      setLoading(true);
      const [totalResponse, confidentResponse, sessionsResponse] = await Promise.all([
        postApi.findAll({}, 0, 1),
        postApi.findAll({ minMacedonianConfidence: MACEDONIAN_CONFIDENCE_THRESHOLD }, 0, 1),
        sessionApi.findAll()
      ]);

      const latestSession = sessionsResponse.data.slice().sort((a, b) => b.id - a.id)[0] ?? null;

      setStats({
        totalPosts: totalResponse.data.totalElements,
        highConfidencePosts: confidentResponse.data.totalElements,
        latestSession
      });
      setLoading(false);
    })();
  }, []);

  const donatedPages = donations.length;

  return (
      <Box sx={{ m: 0, p: 0 }}>
        <Container maxWidth='xl' sx={{ mt: 3, py: 3 }}>
          <Typography variant='h4' gutterBottom>AI Bot for doniraj.vezilka.ai 🤖</Typography>
          <Typography variant='body1' sx={{ mb: 4 }}>
            This bot navigates a social network, extracts Macedonian content and
            donates it to the Vezilka language-preservation platform. Use the
            Sessions page to run the bot, the Posts page to browse what it
            collected, and the Donations page to review and submit batches.
          </Typography>

          {(loading || donationsLoading) && (
              <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress/></Box>
          )}

          {!loading && !donationsLoading && stats && (
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <Card><CardContent>
                    <Typography color='text.secondary' variant='body2'>Extracted Posts</Typography>
                    <Typography variant='h4'>{stats.totalPosts}</Typography>
                  </CardContent></Card>
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <Card><CardContent>
                    <Typography color='text.secondary' variant='body2'>
                      Above {Math.round(MACEDONIAN_CONFIDENCE_THRESHOLD * 100)}% MK Confidence
                    </Typography>
                    <Typography variant='h4'>{stats.highConfidencePosts}</Typography>
                  </CardContent></Card>
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <Card><CardContent>
                    <Typography color='text.secondary' variant='body2'>Donation Batches</Typography>
                    <Typography variant='h4'>{donatedPages}</Typography>
                  </CardContent></Card>
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <Card><CardContent>
                    <Typography color='text.secondary' variant='body2'>Latest Session</Typography>
                    {stats.latestSession
                        ? (<>
                          <Typography variant='h6'>{stats.latestSession.socialNetwork}</Typography>
                          <Chip label={stats.latestSession.status} size='small'/>
                        </>)
                        : <Typography variant='body2' color='text.secondary'>No sessions yet</Typography>}
                  </CardContent></Card>
                </Grid>
              </Grid>
          )}
        </Container>
      </Box>
  );
};

export default HomePage;