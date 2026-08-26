# AWS Notes
- `~/.aws` folder contains AWS configuration and credentials files.
- The files inside, has one or more profiles (the [default] profile is used now).
- `credentials` file contains Access key and Secret access key.
- `config` file contains configuration settings (ex. the `region`).

- go on AWS console, IAM service, and create an access key + secret access key

- `aws configure` -> fill `credentials` and `config`
- IMPORTANT: switch on personal hot-spot (on mobile): there is a company obstacle for https calls
- `aws sts get-caller-identity` -> verify that configuration is correct
y