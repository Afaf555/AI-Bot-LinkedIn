import { Box, Card, CardActions, CardContent, CardMedia, Chip, IconButton, Typography } from '@mui/material';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import DeleteIcon from '@mui/icons-material/Delete';
import { useState } from 'react';
import * as React from 'react';
import { useNavigate } from 'react-router';
import type { PostResponse } from '../../../../api/types/post.ts';
import postApi from '../../../../api/postApi.ts';
import useSnackbar from '../../../../hooks/useSnackbar.ts';

interface PostCardProps {
    post: PostResponse;
}

const PostCard = ({ post }: PostCardProps) => {
    const navigate = useNavigate();
    const { showSnackbar } = useSnackbar();
    const [deleted, setDeleted] = useState<boolean>(false);

    const handleDelete = async (event: React.MouseEvent) => {
        event.stopPropagation();
        try {
            await postApi.delete(post.id.toString());
            setDeleted(true);
        } catch (err) {
            showSnackbar(err instanceof Error ? err.message : 'Failed to delete post.', 'error');
        }
    };

    if (deleted) return null;

    const thumbnail = post.mediaItems[0];

    return (
        <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            {thumbnail && (
                <CardMedia component={thumbnail.type === 'VIDEO' ? 'video' : 'img'} src={thumbnail.sourceUrl}
                           sx={{ height: 160, objectFit: 'cover' }}/>
            )}
            <CardContent sx={{ flexGrow: 1, cursor: 'pointer' }} onClick={() => navigate(`/posts/${post.id}`)}>
                <Typography variant='subtitle2'>{post.authorHandle ?? 'unknown author'}</Typography>
                <Typography variant='body2' color='text.secondary'
                            sx={{ display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                    {post.content ?? 'No content extracted.'}
                </Typography>
                <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 1 }}>
                    {post.macedonianConfidence !== null && (
                        <Chip size='small' label={`MK ${Math.round(post.macedonianConfidence * 100)}%`}/>
                    )}
                    {post.donationBatchId !== null && (
                        <Chip size='small' color='success' label={`Donated (batch #${post.donationBatchId})`}/>
                    )}
                </Box>
            </CardContent>
            <CardActions sx={{ justifyContent: 'space-between' }}>
                {post.sourceUrl
                    ? (
                        <IconButton component='a' href={post.sourceUrl} target='_blank' rel='noreferrer'
                                    aria-label='open source' onClick={(e) => e.stopPropagation()}>
                            <OpenInNewIcon fontSize='small'/>
                        </IconButton>
                    )
                    : <Box/>}
                <IconButton color='error' aria-label='delete' onClick={handleDelete}>
                    <DeleteIcon fontSize='small'/>
                </IconButton>
            </CardActions>
        </Card>
    );
};

export default PostCard;