Select * from users;
-- 23177662 Chinthana 
-- 98093514 Anup
-- 113047789 Cher
-- 614320 Steven

Select count(*) from activities where user_id = 23177662;
Select * from activities limit 10;

Select count(*) from activities where user_id = 614320 and avg_heartrate_bpm is not null and avg_cadence is not null;

select * from weekly_summary where user_id = 98093514 order by week_start desc;
--delete from weekly_summary where user_id = 98093514;

-- weekly_summary previous
-- 439 Chinthana > 155
-- 204 Anup > 132 

select count(*) from weekly_summary where user_id = 23177662
select * from user_clubs;
Select * from clubs;

