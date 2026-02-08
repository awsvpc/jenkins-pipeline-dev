AWSTemplateFormatVersion: '2010-09-09'
Description: 'This is to create ECS Cluster, ECS Security Groups and ECS Task Execution Role'

Metadata:
  'AWS::CloudFormation::Interface':
    ParameterGroups:
    - Label:
        default: 'VPC Parameters'
      Parameters:
      - VPCId
      
Parameters:
  VPCId:
    Description: 'Provide VPC ID'
    Type: 'AWS::EC2::VPC::Id'
    ConstraintDescription: This must be an existing VPC within the working region.
    
Resources:
  ECSCluster:
    Type: AWS::ECS::Cluster
    Properties: 
        ClusterName: !Sub 'artifactory-ecs-cluster'
        Tags:
        - Key: Name
          Value: !Sub 'artifactory-ecs-cluster'
          
  ECSSecurityGroup:
    Type: 'AWS::EC2::SecurityGroup'
    Properties:
      GroupDescription: ECS security group
      VpcId: !Ref VPCId 
  
  # Task execution role provides permission to Amazon ECS container and Fargate agents
  # to pull container image from ECR and use awslogs log driver
  ECSTaskExecutionRole:
    Type: AWS::IAM::Role
    Properties:
      RoleName: task_execution_role
      AssumeRolePolicyDocument:
        Statement:
          - Effect: Allow
            Principal:
              Service:
                - ecs.amazonaws.com
                - ecs-tasks.amazonaws.com
                - events.amazonaws.com
            Action: [ 'sts:AssumeRole' ]
      Path: /
      Policies:
        - PolicyName: !Join
            - ""
            - - !FindInMap [ Constant, Service, Name ]
              - '_task_execution_policy'
          PolicyDocument:
            Statement:
              - Sid: ecrRole
                Effect: Allow
                Action:
                  # Allow the ECS interact with container images
                  - 'ecr:GetAuthorizationToken'
                  - 'ecr:BatchCheckLayerAvailability'
                  - 'ecr:GetDownloadUrlForLayer'
                  - 'ecr:BatchGetImage'
                Resource: '*'

              - Sid: cloudWatchLogsRole
                Effect: Allow
                Action:
                  # Allow the ECS tasks to publish logs to CloudWatch
                  - 'logs:CreateLogStream'
                  - 'logs:CreateLogGroup'
                  - 'logs:PutLogEvents'
                  - 'logs:DescribeLogStreams'
                Resource: '*'

Outputs:
  ECSCluster:
    Description: The name of the ECS cluster
    Value: !Ref ECSCluster
    Export:
      Name: 'artifactory-ecs-cluster-name' 
  ECSSecurityGroup:
    Value: !Ref ECSSecurityGroup
    Description: 'ECS Default Security Group ID'
    Export:
      Name: 'artifactory-ecs-security-group-id'
  ECSTaskExecutionRole:
    Description: The name of the ECS cluster
    Value: !Ref ECSTaskExecutionRole
    Export:
      Name: 'task_execution_role' 
@awsvpc
Comment
