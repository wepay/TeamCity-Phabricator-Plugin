<?php

final class HarbormasterTeamCityBuildStepImplementation
  extends HarbormasterBuildStepImplementation {

  public function getName() {
    return pht('Build with TeamCity');
  }

  public function getGenericDescription() {
    return pht('Trigger TeamCity Builds with Harbormaster');
  }

  public function getBuildStepGroupKey() {
    return HarbormasterExternalBuildStepGroup::GROUPKEY;
  }

  public function getDescription() {
    $domain = null;
    $uri = $this->getSetting('uri');
    if ($uri) {
      $domain = id(new PhutilURI($uri))->getDomain();
    }

    $method = $this->formatSettingForDescription('method', 'POST');
    $domain = $this->formatValueForDescription($domain);

    if ($this->getSetting('credential')) {
      return pht(
        'Make an authenticated HTTP %s request to %s.',
        $method,
        $domain);
    } else {
      return pht(
        'Make an HTTP %s request to %s.',
        $method,
        $domain);
    }
  }

  public function execute(
    HarbormasterBuild $build,
    HarbormasterBuildTarget $build_target) {

    $viewer = PhabricatorUser::getOmnipotentUser();
    $settings = $this->getSettings();
    $variables = $build_target->getVariables();

    $uri = $settings['uri'] . '/httpAuth/app/rest/buildQueue';

    $method = 'POST';
    $contentType = 'application/xml';

    $xmlBuilder = new TeamCityXmlBuildBuilder();
    $payload = $xmlBuilder
        ->addBuildId($settings['buildId'])
        ->addBranchName(implode(array("D", $variables['buildable.diff'])))
        ->addDiffId(implode(array("D", $variables['buildable.diff'])))
        ->addHarbormasterPHID($variables['target.phid'])
        ->addRevisionId($variables['buildable.revision'])
        ->build();

    $future = id(new HTTPFuture($uri, $payload))
      ->setMethod($method)
      ->addHeader('Content-Type', $contentType)
      ->setTimeout(60);

    $credential_phid = $this->getSetting('credential');
    if ($credential_phid) {
<<<<<<< Updated upstream
      $key = PassphrasePasswordKey::loadFromPHID(
        $credential_phid,
        $viewer);
      $future->setHTTPBasicAuthCredentials(
        $key->getUsernameEnvelope()->openEnvelope(),
        $key->getPasswordEnvelope());
=======
       $key = PassphrasePasswordKey::loadFromPHID(
              $credential_phid,
              $viewer);
       $username = $key->getUsernameEnvelope()->openEnvelope();
       $password = $key->getPasswordEnvelope()->openEnvelope();
       curl_setopt($process, CURLOPT_USERPWD, $username . ":" . $password);
>>>>>>> Stashed changes
    }

    $this->resolveFutures(
      $build,
      $build_target,
      array($future));

    list($status, $body, $headers) = $future->resolve();

    $header_lines = array();

    // TODO: We don't currently preserve the entire "HTTP" response header, but
    // should. Once we do, reproduce it here faithfully.
    $status_code = $status->getStatusCode();
    $header_lines[] = "HTTP {$status_code}";

    foreach ($headers as $header) {
      list($head, $tail) = $header;
      $header_lines[] = "{$head}: {$tail}";
    }
    $header_lines = implode("\n", $header_lines);

    //$build_target
    //  ->newLog($uri, 'http.head')
    //  ->append($headers);

    //$build_target
    //  ->newLog($uri, 'http.body')
    //  ->append($body);

    if ($status === 200) {
      throw new HarbormasterBuildFailureException();
    }
  }

  public function getFieldSpecifications() {
    return array(
      'uri' => array(
        'name' => pht('URI'),
        'type' => 'text',
        'required' => true,
      ),
      'buildId' => array(
        'name' => pht('TeamCity Build Configuration ID'),
        'type' => 'text',
        'required' => true,
      ),
      'credential' => array(
          'name' => pht('TeamCity Credentials'),
          'type' => 'credential',
          'required' => true,
          'credential.type'
          => PassphrasePasswordCredentialType::CREDENTIAL_TYPE,
          'credential.provides'
          => PassphrasePasswordCredentialType::PROVIDES_TYPE,
      ),
    );
  }

  public function supportsWaitForMessage() {
    return true;
  }

}
