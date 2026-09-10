import { useCallback, useEffect, useState } from 'react';
import { Box, Card, CardMedia, Chip, CircularProgress, Link as MuiLink, Stack, Typography } from '@mui/material';
import { useParams } from 'react-router';
import type { PostResponse } from '../../../../api/types/post.ts';
import postApi from '../../../../api/postApi.ts';
import useSnackbar from '../../../../hooks/useSnackbar.ts';

const PostDetailsPage = () => {
    const { id } = useParams<{ id: string }>();
    const { showSnackbar } = useSnackbar();

    const [post, setPost] = useState<PostResponse | null>(null);
    const [loading, setLoading] = useState<boolean>(true);

    const fetch = useCallback(async () => {
        setLoading(true);
        try {
            const response = await postApi.findById(id!);
            setPost(response.data);
        } catch (err) {
            showSnackbar(err instanceof Error ? err.message : 'Failed to load post.', 'error');
        } finally {
            setLoading(false);
        }
    }, [id, showSnackbar]);

    useEffect(() => { void fetch(); }, [fetch]);

    if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress/></Box>;
    if (!post) return <Typography color='text.secondary'>Post #{id} not found.</Typography>;

    return (
        <Box>
            <Typography variant='h5' gutterBottom>Post #{id}</Typography>
            <Stack spacing={1} sx={{ mb: 2 }}>
                <Typography variant='subtitle1'>{post.authorHandle ?? 'unknown author'}</Typography>
                <Typography variant='body1'>{post.content ?? 'No content extracted.'}</Typography>
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    <Chip label={post.socialNetwork} size='small'/>
                    {post.macedonianConfidence !== null && (
                        <Chip label={`MK confidence ${Math.round(post.macedonianConfidence * 100)}%`} size='small'/>
                    )}
                    <Chip
                        label={post.donationBatchId !== null ? `Donated (batch #${post.donationBatchId})` : 'Not donated'}
                        color={post.donationBatchId !== null ? 'success' : 'default'} size='small'/>
                </Box>
                {post.postedAt && (
                    <Typography variant='body2' color='text.secondary'>
                        Posted at: {new Date(post.postedAt).toLocaleString()}
                    </Typography>
                )}
                {post.sourceUrl && (
                    <MuiLink href={post.sourceUrl} target='_blank' rel='noreferrer'>View original source</MuiLink>
                )}
            </Stack>

            {post.mediaItems.length > 0 && (
                <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                    {post.mediaItems.map((media) => (
                        <Card key={media.id} sx={{ width: 240 }}>
                            <CardMedia component={media.type === 'VIDEO' ? 'video' : 'img'} src={media.sourceUrl}
                                       controls={media.type === 'VIDEO'} sx={{ height: 180, objectFit: 'cover' }}/>
                        </Card>
                    ))}
                </Box>
            )}
        </Box>
    );
};

export default PostDetailsPage;