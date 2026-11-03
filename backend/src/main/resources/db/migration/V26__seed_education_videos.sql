-- Curated, idempotent starter video library. Links point to the original UCSF-hosted YouTube videos.
INSERT INTO wellness.education_resources
    (slug, title, topic, summary, content, content_type, resource_url, published, status, display_order)
VALUES
('video-grounding-emotions','Emotion Regulation and Grounding','Grounding',
 'A practical introduction to grounding and regulating difficult emotions, presented by Sarah Metz, PsyD.',
 'Educational video from the UCSF Department of Psychiatry and Behavioral Sciences. Use this as general learning material; it is not a substitute for individual clinical advice.',
 'VIDEO','https://www.youtube.com/watch?v=Mj9PcYbpF8U',TRUE,'PUBLISHED',110),
('video-mindful-breathing','Brief Mindful Breathing for Anxiety','Anxiety',
 'A short guided mindful breathing resource presented by Christiane Wolf, MD, PhD.',
 'Educational video from the UCSF Department of Psychiatry and Behavioral Sciences. Stop if the exercise feels uncomfortable and return to your normal breathing.',
 'VIDEO','https://www.youtube.com/watch?v=tc0oVElWR5I',TRUE,'PUBLISHED',120),
('video-deep-breathing-stress','Deep Breathing for Stress Reduction','Stress',
 'Learn how deep breathing can be used as one practical tool for managing stress, presented by Elissa Epel, PhD.',
 'Educational video from the UCSF Department of Psychiatry and Behavioral Sciences. Breathing exercises are optional; choose a comfortable pace.',
 'VIDEO','https://www.youtube.com/watch?v=WN663nOWQ9A',TRUE,'PUBLISHED',130),
('video-sleep-wellbeing','Getting a Good Night’s Sleep','Sleep',
 'A learning resource about sleep and wellbeing featuring Aric A. Prather, PhD.',
 'Educational video from the UCSF Department of Psychiatry and Behavioral Sciences. Persistent sleep difficulties may benefit from discussion with a qualified health professional.',
 'VIDEO','https://www.youtube.com/watch?v=lJqerySuYsg',TRUE,'PUBLISHED',140)
ON CONFLICT (slug) DO NOTHING;
