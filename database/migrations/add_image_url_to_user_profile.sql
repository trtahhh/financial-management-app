-- Add imageUrl column to User_Profile table
ALTER TABLE User_Profile 
ADD imageUrl NVARCHAR(500) NULL;

-- Add comment for the new column
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'URL for user profile image', 
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'User_Profile',
    @level2type = N'COLUMN', @level2name = N'imageUrl';
