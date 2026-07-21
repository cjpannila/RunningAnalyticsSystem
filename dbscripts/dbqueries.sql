Select * from users;
-- 23177662 Chinthana 
-- 98093514 Anup
-- 113047789 Cher
-- 614320 Steven
-- 7211094 Alan
-- 13419295 Pradeep
-- 32658279 Greg

--update users set country = 'Australia' where user_id = 32658279;

Select * from activities limit 10;
Select count(*) from activities where user_id = 13419295;
Select count(*) from activities where user_id = 13419295 and activity_type = 'Run';
Select count(*) from activities where user_id = 13419295 and avg_heartrate_bpm is not null and avg_cadence is not null and activity_type = 'Run';

select * from weekly_summary where user_id = 98093514 order by week_start desc;
--delete from weekly_summary where user_id = 98093514;

-- weekly_summary previous
-- 439 Chinthana > 155
-- 204 Anup > 132 

select count(*) from weekly_summary where user_id = 23177662
select * from user_clubs where user_id = 7211094;
Select * from clubs;

