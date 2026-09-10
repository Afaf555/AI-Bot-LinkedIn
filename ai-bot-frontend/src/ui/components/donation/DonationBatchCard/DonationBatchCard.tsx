import { Button, Card, CardActions, CardContent, Chip, Stack, Typography } from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import SendIcon from '@mui/icons-material/Send';
import { useState } from 'react';
import type { DonationBatchResponse, DonationStatus } from '../../../../api/types/donation.ts';
import donationApi from '../../../../api/donationApi.ts';
import useSnackbar from '../../../../hooks/useSnackbar.ts';

interface DonationBatchCardProps {
  batch: DonationBatchResponse;
}

const statusColor: Record<DonationStatus, 'default' | 'info' | 'warning' | 'success' | 'error'> = {
  DRAFT: 'default', APPROVED: 'info', SUBMITTED: 'warning', ACCEPTED: 'success', REJECTED: 'error', FAILED: 'error'
};

const DonationBatchCard = ({ batch: initialBatch }: DonationBatchCardProps) => {
  const { showSnackbar } = useSnackbar();
  const [batch, setBatch] = useState<DonationBatchResponse>(initialBatch);
  const [submitting, setSubmitting] = useState<boolean>(false);

  const handleApprove = async () => {
    setSubmitting(true);
    try {
      const response = await donationApi.approve(batch.id.toString());
      setBatch(response.data);
    } catch (err) {
      showSnackbar(err instanceof Error ? err.message : 'Failed to approve batch.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const response = await donationApi.submit(batch.id.toString());
      setBatch(response.data);
    } catch (err) {
      showSnackbar(err instanceof Error ? err.message : 'Failed to submit batch.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
      <Card>
        <CardContent>
          <Typography variant='h6'>Batch #{batch.id}</Typography>
          <Chip label={batch.status} color={statusColor[batch.status]} size='small' sx={{ mb: 1 }}/>
          <Stack spacing={0.5}>
            <Typography variant='body2' color='text.secondary'>{batch.postIds.length} post(s)</Typography>
            <Typography variant='body2' color='text.secondary'>Created: {new Date(batch.createdAt).toLocaleString()}</Typography>
            {batch.submittedAt && (
                <Typography variant='body2' color='text.secondary'>Submitted: {new Date(batch.submittedAt).toLocaleString()}</Typography>
            )}
            {batch.vezilkaReference && (
                <Typography variant='body2' color='text.secondary'>Vezilka reference: {batch.vezilkaReference}</Typography>
            )}
          </Stack>
        </CardContent>
        <CardActions sx={{ justifyContent: 'flex-end' }}>
          <Button size='small' startIcon={<CheckIcon/>} disabled={batch.status !== 'DRAFT' || submitting} onClick={handleApprove}>
            Approve
          </Button>
          <Button size='small' startIcon={<SendIcon/>} disabled={batch.status !== 'APPROVED' || submitting} onClick={handleSubmit}>
            Submit
          </Button>
        </CardActions>
      </Card>
  );
};

export default DonationBatchCard;